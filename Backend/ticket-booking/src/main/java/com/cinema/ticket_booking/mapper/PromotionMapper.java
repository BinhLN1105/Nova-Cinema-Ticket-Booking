package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.request.PromotionRequest;
import com.cinema.ticket_booking.dto.response.PromotionResponse;
import com.cinema.ticket_booking.model.Promotion;
import org.mapstruct.*;

import java.time.LocalTime;

@Mapper(componentModel = "spring", imports = {LocalTime.class})
public interface PromotionMapper {

    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    PromotionResponse toResponse(Promotion entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "startDate", expression = "java(request.getStartDate() != null ? request.getStartDate().atStartOfDay() : null)")
    @Mapping(target = "endDate", expression = "java(request.getEndDate() != null ? request.getEndDate().atTime(LocalTime.MAX) : null)")
    Promotion toEntity(PromotionRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "startDate", expression = "java(request.getStartDate() != null ? request.getStartDate().atStartOfDay() : null)")
    @Mapping(target = "endDate", expression = "java(request.getEndDate() != null ? request.getEndDate().atTime(LocalTime.MAX) : null)")
    void updateEntity(PromotionRequest request, @MappingTarget Promotion entity);
}
