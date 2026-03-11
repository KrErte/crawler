package ee.itjobs.mapper;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.entity.Job;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface JobMapper {
    JobDto toDto(Job job);
    List<JobDto> toDtoList(List<Job> jobs);
}
