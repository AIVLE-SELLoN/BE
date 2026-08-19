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
import com.aivle.sellon.rawdb.entity.RawMappedData;
import com.aivle.sellon.rawdb.entity.RawProduct;
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

// 상품 매칭 도커 툴(docker_mapping_tool)과 주고받는 배치 export/import + 채널 상품 카탈로그 적재
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

    // input_channel_products.csv를 raw db에 적재 - 회사 단위로 받아 channel 컬럼으로 UsersChannel을 찾고, 미연동 채널 행은 스킵
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
                        record.get("variant_row_id"),
                        channel,
                        record.get("channel_product_id"),
                        record.get("channel_product_name"),
                        record.isMapped("option_group_names") ? record.get("option_group_names") : null,
                        record.isMapped("channel_option_name") ? record.get("channel_option_name") : null,
                        parseIntegerOrNull(record, "sale_price"),
                        parseIntegerOrNull(record, "original_price")
                );
                count++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return count;
    }

    // 회사가 연동한 모든 채널의 미매칭 상품을 매칭 툴 input_channel_products.csv 포맷으로 내보냄 (크로스채널 매칭용)
    @Transactional(readOnly = true)
    public byte[] exportUnmatchedCsv(Long companyId) {
        List<String> channelTypes = usersChannelRepository.findByCompany_Id(companyId).stream()
                .map(UsersChannel::getChannelType)
                .distinct()
                .toList();

        List<RawProduct> products = rawChannelProductMappingService.getProductsByChannelIds(channelTypes);
        Map<String, RawMappedData> mappings = rawChannelProductMappingService.getMappingsByVariantRowIds(
                products.stream().map(RawProduct::getVariantRowId).toList());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 엑셀이 UTF-8을 시스템 기본 인코딩(CP949)으로 오인해 한글이 깨지는 것을 막기 위해 BOM을 붙인다.
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(EXPORT_HEADERS).build())) {
            for (RawProduct p : products) {
                RawMappedData mapping = mappings.get(p.getVariantRowId());
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

    // mapping_result.csv import - 같은 (channel, channel_product_id)의 variant 전부를 한번에 확정 처리
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

                // 실제 갱신이 없었다면(MANUAL 보호) 쓸모없는 MasterProduct를 만들지 않는다.
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

    // review_queue.csv(보류 판정)는 자동 확정하지 않고 ProductMappingReviewItem으로 저장 (사람이 별도 resolve)
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

    private Integer parseIntegerOrNull(CSVRecord record, String column) {
        if (!record.isMapped(column)) {
            return null;
        }
        String value = record.get(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 엑셀 저장 CSV에 붙는 UTF-8 BOM을 파싱 전에 제거 (안 지우면 첫 헤더 매칭 실패)
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
