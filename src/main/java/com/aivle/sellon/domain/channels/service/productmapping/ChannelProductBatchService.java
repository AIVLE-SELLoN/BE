package com.aivle.sellon.domain.channels.service.productmapping;

import com.aivle.sellon.domain.channels.entity.productmapping.ChannelProduct;
import com.aivle.sellon.domain.channels.entity.productmapping.MasterProduct;
import com.aivle.sellon.domain.channels.entity.productmapping.ProductMappingReviewItem;
import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.enums.MappingMethod;
import com.aivle.sellon.domain.channels.enums.MappingStatus;
import com.aivle.sellon.domain.channels.exception.ChannelAccessDeniedException;
import com.aivle.sellon.domain.channels.exception.connection.UsersChannelNotFoundException;
import com.aivle.sellon.domain.channels.repository.productmapping.ChannelProductRepository;
import com.aivle.sellon.domain.channels.repository.productmapping.MasterProductRepository;
import com.aivle.sellon.domain.channels.repository.productmapping.ProductMappingReviewItemRepository;
import com.aivle.sellon.domain.channels.repository.connection.UsersChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 상품 매칭 도커 툴(docker_mapping_tool)과 주고받는 배치 export/import.
 * 그 툴은 CLI 배치 도구라 팀원이 각자 수동으로 컨테이너를 돌리고, 그 결과 CSV를 이 서비스에 업로드하는 구조.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelProductBatchService {

    private static final String[] EXPORT_HEADERS = {
            "variant_row_id", "channel", "channel_product_id", "channel_product_name",
            "option_group_names", "channel_option_name", "sale_price", "original_price"
    };

    private final ChannelProductRepository channelProductRepository;
    private final MasterProductRepository masterProductRepository;
    private final UsersChannelRepository usersChannelRepository;
    private final ProductMappingReviewItemRepository productMappingReviewItemRepository;

    /**
     * 미매칭 상품을 매칭 툴 input_channel_products.csv 포맷으로 내보낸다.
     */
    @Transactional(readOnly = true)
    public byte[] exportUnmatchedCsv(Long companyId, Long usersChannelKey) {
        UsersChannel usersChannel = getOwnedUsersChannelOrThrow(usersChannelKey, companyId);

        List<ChannelProduct> unmatched = channelProductRepository
                .findByUsersChannel_UsersChannelKeyAndMappingStatus(usersChannelKey, MappingStatus.UNMATCHED);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(EXPORT_HEADERS).build())) {
            for (ChannelProduct cp : unmatched) {
                printer.printRecord(
                        cp.getSourceSku(),
                        usersChannel.getChannelType(),
                        cp.getChannelItemId(),
                        cp.getProductName(),
                        cp.getOptionGroupNames(),
                        cp.getOptionName(),
                        cp.getPrice(),
                        cp.getOriginalPrice()
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /**
     * mapping_result.csv(channel, channel_product_id, product_name, median_price, product_key, mapped_product_code, cluster_size) 를 import.
     * 한 줄 = channelItemId 하나 = 이미 저희 쪽 그룹핑 단위와 동일해서, 같은 channelItemId를 공유하는 옵션 전부를 한번에 확정 처리한다.
     */
    @Transactional
    public int importMappingResult(Long companyId, Long usersChannelKey, MultipartFile file) {
        UsersChannel usersChannel = getOwnedUsersChannelOrThrow(usersChannelKey, companyId);

        int matchedCount = 0;
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

            for (CSVRecord record : parser) {
                String channelItemId = record.get("channel_product_id");
                String mappedProductCode = record.get("mapped_product_code");
                if (mappedProductCode == null || mappedProductCode.isBlank()) {
                    // 매칭 실패/미확정 행은 건너뛴다 (UNMATCHED로 유지)
                    continue;
                }
                String productName = record.isMapped("product_name") ? record.get("product_name") : null;

                MasterProduct masterProduct = masterProductRepository.findByMasterSku(mappedProductCode)
                        .orElseGet(() -> masterProductRepository.save(
                                MasterProduct.of(usersChannel.getCompany(), mappedProductCode,
                                        productName != null ? productName : mappedProductCode)));

                List<ChannelProduct> siblings = channelProductRepository
                        .findByUsersChannel_UsersChannelKeyAndChannelItemId(usersChannelKey, channelItemId);

                for (ChannelProduct cp : siblings) {
                    if (cp.getMappingStatus() != MappingStatus.UNMATCHED) {
                        continue;
                    }
                    // 매칭 결과 CSV에는 rule/embedding 구분 컬럼이 없어 Plan A(임베딩 우선) 기준으로 태깅
                    cp.autoMatch(masterProduct, null, MappingMethod.EMBEDDING, MappingStatus.AUTO_MATCHED);
                    matchedCount++;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return matchedCount;
    }

    /**
     * review_queue.csv(보류 판정 상품 쌍)는 자동 확정하지 않고 ProductMappingReviewItem으로 저장해
     * 사람이 별도 화면/API에서 확인·resolve 처리하도록 한다.
     * 두 상품이 서로 다른 채널에 속할 수 있어 Company 단위로 저장한다.
     */
    @Transactional
    public int importReviewQueue(Long companyId, Long usersChannelKey, MultipartFile file) {
        UsersChannel usersChannel = getOwnedUsersChannelOrThrow(usersChannelKey, companyId);
        int rowCount = 0;
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

            for (CSVRecord record : parser) {
                ProductMappingReviewItem item = ProductMappingReviewItem.of(
                        usersChannel.getCompany(),
                        record.get("channel_a"), record.get("product_key_a"),
                        record.get("channel_b"), record.get("product_key_b"),
                        parseDoubleOrNull(record, "rule_score"),
                        parseDoubleOrNull(record, "emb_score"),
                        record.isMapped("verdict") ? record.get("verdict") : null,
                        record.isMapped("basis") ? record.get("basis") : null
                );
                productMappingReviewItemRepository.save(item);
                rowCount++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rowCount;
    }

    private Double parseDoubleOrNull(CSVRecord record, String column) {
        if (!record.isMapped(column)) {
            return null;
        }
        String value = record.get(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private UsersChannel getOwnedUsersChannelOrThrow(Long usersChannelKey, Long companyId) {
        UsersChannel usersChannel = usersChannelRepository.findById(usersChannelKey)
                .orElseThrow(UsersChannelNotFoundException::new);
        if (!usersChannel.getCompany().getId().equals(companyId)) {
            throw new ChannelAccessDeniedException();
        }
        return usersChannel;
    }
}
