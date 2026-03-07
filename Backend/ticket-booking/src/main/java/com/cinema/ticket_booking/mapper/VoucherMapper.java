package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.request.VoucherRequest;
import com.cinema.ticket_booking.dto.response.VoucherResponse;
import com.cinema.ticket_booking.model.Voucher;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VoucherMapper {

    @Mapping(target = "id", expression = "java(voucher.getId().toString())")
    VoucherResponse toResponse(Voucher voucher);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usedCount", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Voucher toEntity(VoucherRequest request);

    @Mapping(target = "code", source = "code")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "discountType", source = "discountType")
    @Mapping(target = "discountValue", source = "discountValue")
    @Mapping(target = "maxDiscount", source = "maxDiscount")
    @Mapping(target = "minOrder", source = "minOrder")
    @Mapping(target = "validTo", source = "validTo")
    VoucherResponse.Summary toSummary(Voucher voucher);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usedCount", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(VoucherRequest request, @MappingTarget Voucher voucher);
}
