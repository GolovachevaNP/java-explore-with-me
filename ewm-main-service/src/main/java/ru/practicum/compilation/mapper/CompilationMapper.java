package ru.practicum.compilation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.model.Compilation;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    @Mapping(target = "events", ignore = true)
    CompilationDto toCompilationDto(Compilation compilation);
}