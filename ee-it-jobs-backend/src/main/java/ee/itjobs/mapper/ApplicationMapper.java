package ee.itjobs.mapper;

import ee.itjobs.dto.application.ApplicationDto;
import ee.itjobs.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApplicationMapper {
    @Mapping(source = "job.id", target = "jobId")
    @Mapping(source = "job.title", target = "jobTitle")
    @Mapping(source = "job.company", target = "company")
    @Mapping(source = "job.url", target = "jobUrl")
    @Mapping(source = "job.source", target = "source")
    ApplicationDto toDto(Application application);

    List<ApplicationDto> toDtoList(List<Application> applications);
}
