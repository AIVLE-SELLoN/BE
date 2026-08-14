package com.aivle.sellon.domain.channels.service.productmapping;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.entity.productmapping.MasterProduct;
import com.aivle.sellon.domain.channels.entity.productmapping.ProductMappingReviewItem;
import com.aivle.sellon.domain.channels.enums.MappingMethod;
import com.aivle.sellon.domain.channels.exception.ChannelAccessDeniedException;
import com.aivle.sellon.domain.channels.exception.connection.UsersChannelNotFoundException;
import com.aivle.sellon.domain.channels.repository.connection.UsersChannelRepository;
import com.aivle.sellon.domain.channels.repository.productmapping.MasterProductRepository;
import com.aivle.sellon.domain.channels.repository.productmapping.ProductMappingReviewItemRepository;
import com.aivle.sellon.rawdb.entity.ChannelProductMapping;
import com.aivle.sellon.rawdb.entity.RawChannelProduct;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 상품 매칭 도커 툴(docker_mapping_tool)과 주고받는 배치 export/import, 그리고 Mock Producer가
 * 내려주는 채널 상품 카탈로그(input_channel_products.csv) 적재.
 * 그 매칭 툴은 CLI 배치 도구라 팀원이 각자 수동으로 컨테이너를 돌리고, 그 결과 CSV를 이 서비스에 업로드하는 구조.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelProductBatchService {

    private static final String[] EXPORT_HEADERS = {
            "variant_row_id", "channel", "channel_product_id", "channel_product_name",
            "option_group_names", "channel_option_name", "sale_price", "original_price"
    };

    private final RawChannelProductMappingService rawChannelProductMappingService;
    private final MasterProductRepository masterProductRepository;
    private final UsersChannelRepository usersChannelRepository;
    private final ProductMappingReviewItemRepository productMappingReviewItemRepository;

    /**
     * Mock Producer의 input_channel_products.csv(variant_row_id, channel, channel_product_id,
     * channel_product_name, option_group_names, channel_option_name, sale_price, original_price)를
     * raw db(products/mapped_data)에 적재한다.
     * 여러 채널 상품이 한 파일에 섞여 있어(크로스채널 매칭 전제) usersChannel이 아닌 회사 단위로 받는다 -
     * 행마다 channel 컬럼으로 그 회사의 연동된 UsersChannel을 찾고, 연동 안 된 채널의 행은 건너뛴다.
     */
    @Transactional(readOnly = true)
    public int importChannelProducts(Long companyId, MultipartFile file) {
        Map<String, UsersChannel> channelMap = usersChannelRepository.findByCompany_Id(companyId).stream()
                .collect(Collectors.toMap(UsersChannel::getChannelType, Function.identity()));

        int count = 0;
        try (InputStreamReader reader = new InputStreamReader(stripBom(file.getInputStream()), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

            for (CSVRecord record : parser) {
                String channel = record.get("channel");
                UsersChannel usersChannel = channelMap.get(channel);
                if (usersChannel == null) {
                    // 연동되지 않은 채널의 상품 행은 스킵
                    continue;
                }
                rawChannelProductMappingService.upsertProduct(
                        usersChannel.getUsersChannelKey(),
                        record.get("variant_row_id"),
                        channel,
                        record.get("channel_product_id"),
                        record.get("channel_product_name"),
                        record.isMapped("option_group_names") ? record.get("option_group_names") : null,
                        record.isMapped("channel_option_name") ? record.get("channel_option_name") : null,
                        parseLongOrNull(record, "sale_price"),
                        parseLongOrNull(record, "original_price")
                );
                count++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return count;
    }

    /**
     * 미매칭 상품을 매칭 툴 input_channel_products.csv 포맷으로 내보낸다.
     * 매핑 목적이 서로 다른 채널 상품을 하나의 그룹으로 묶는 것(크로스채널 매칭)이라, 특정 채널 하나가
     * 아니라 회사가 연동한 모든 채널의 미매칭 상품을 한 파일에 같이 담아야 매칭 툴이 채널 간 비교를 할 수 있다.
     */
    @Transactional(readOnly = true)
    public byte[] exportUnmatchedCsv(Long companyId) {
        List<Long> usersChannelKeys = usersChannelRepository.findByCompany_Id(companyId).stream()
                .map(UsersChannel::getUsersChannelKey)
                .toList();

        List<RawChannelProduct> products = rawChannelProductMappingService.getProducts(usersChannelKeys);
        Map<String, ChannelProductMapping> mappings = rawChannelProductMappingService.getMappingsByVariantRowIds(
                products.stream().map(RawChannelProduct::getVariantRowId).toList());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(EXPORT_HEADERS).build())) {
            for (RawChannelProduct p : products) {
                ChannelProductMapping mapping = mappings.get(p.getVariantRowId());
                if (mapping != null && mapping.getProductGroupId() != null) {
                    continue;
                }
                printer.printRecord(
                        p.getVariantRowId(),
                        p.getChannelId(),
                        p.getChannelProductId(),
                        p.getChannelProductName(),
                        p.getOptionGroupNames(),
                        p.getChannelOptionName(),
                        p.getSalePrice(),
                        p.getOriginalPrice()
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /**
     * mapping_result.csv(channel, channel_product_id, product_name, median_price, product_key, mapped_product_code, cluster_size) 를 import.
     * 한 줄 = (channel, channel_product_id) 하나 = raw db mapped_data 기준 그룹핑 단위와 동일해서,
     * 같은 (channel, channel_product_id)를 공유하는 옵션(variant) 전부를 한번에 확정 처리한다.
     * 클러스터가 여러 채널에 걸칠 수 있어 행마다 CSV의 channel 컬럼을 그대로 쓴다(path의 usersChannelKey는 업로드 주체 인증용).
     */
    @Transactional
    public int importMappingResult(Long companyId, Long usersChannelKey, MultipartFile file) {
        UsersChannel usersChannel = getOwnedUsersChannelOrThrow(usersChannelKey, companyId);

        int matchedCount = 0;
        try (InputStreamReader reader = new InputStreamReader(stripBom(file.getInputStream()), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

            for (CSVRecord record : parser) {
                String channel = record.isMapped("channel") ? record.get("channel") : usersChannel.getChannelType();
                String channelProductId = record.get("channel_product_id");
                String mappedProductCode = record.get("mapped_product_code");
                if (mappedProductCode == null || mappedProductCode.isBlank()) {
                    // 매칭 실패/미확정 행은 건너뛴다 (UNMATCHED로 유지)
                    continue;
                }
                String productName = record.isMapped("product_name") ? record.get("product_name") : null;

                int confirmed = rawChannelProductMappingService.confirmMappingForChannelProduct(
                        channel, channelProductId, mappedProductCode, MappingMethod.EMBEDDING, null);

                // 사람 개입 우선 정책으로 전부 MANUAL 보호되어 실제 갱신이 없었다면(confirmed == 0),
                // 아무 데도 안 쓰이는 MasterProduct(product_group_id)를 낭비하지 않도록 생성하지 않는다.
                if (confirmed > 0) {
                    masterProductRepository.findByCompany_IdAndProductGroupId(companyId, mappedProductCode)
                            .orElseGet(() -> masterProductRepository.save(
                                    MasterProduct.of(usersChannel.getCompany(), mappedProductCode,
                                            productName != null ? productName : mappedProductCode)));
                }

                matchedCount += confirmed;
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
        try (InputStreamReader reader = new InputStreamReader(stripBom(file.getInputStream()), StandardCharsets.UTF_8);
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

    private Long parseLongOrNull(CSVRecord record, String column) {
        if (!record.isMapped(column)) {
            return null;
        }
        String value = record.get(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 엑셀 등에서 저장한 CSV는 앞에 보이지 않는 UTF-8 BOM(EF BB BF)이 붙는 경우가 많다.
     * BOM이 그대로 남으면 첫 헤더("variant_row_id" 등) 앞에 붙어버려 정확한 컬럼명 매칭이
     * 실패하므로(Mapping for variant_row_id not found), 파싱 전에 미리 걷어낸다.
     */
    private InputStream stripBom(InputStream in) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(in, 3);
        byte[] bom = new byte[3];
        int read = pushbackInputStream.read(bom, 0, 3);
        if (read != 3 || bom[0] != (byte) 0xEF || bom[1] != (byte) 0xBB || bom[2] != (byte) 0xBF) {
            pushbackInputStream.unread(bom, 0, Math.max(read, 0));
        }
        return pushbackInputStream;
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
