package com.mootann.arxivdaily.converter;

import com.mootann.arxivdaily.dto.arxiv.ArxivPaperDTO;
import com.mootann.arxivdaily.model.ArxivPaper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ArxivPaperStructMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdTime", ignore = true)
    @Mapping(target = "updatedTime", ignore = true)
    ArxivPaper toEntity(ArxivPaperDTO dto);

    ArxivPaperDTO toDto(ArxivPaper entity);

    List<ArxivPaper> toEntityList(List<ArxivPaperDTO> dtos);

    List<ArxivPaperDTO> toDtoList(List<ArxivPaper> entities);
}