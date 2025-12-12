package com.techstore.service;

import com.techstore.dto.request.ParameterOptionRequestDto;
import com.techstore.dto.request.ParameterOrderDto;
import com.techstore.dto.request.ParameterRequestDto;
import com.techstore.dto.response.ParameterResponseDto;
import com.techstore.entity.Category;
import com.techstore.entity.Parameter;
import com.techstore.entity.ParameterOption;
import com.techstore.exception.BusinessLogicException;
import com.techstore.exception.DuplicateResourceException;
import com.techstore.exception.ValidationException;
import com.techstore.mapper.ParameterMapper;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ParameterOptionRepository;
import com.techstore.repository.ParameterRepository;
import com.techstore.util.ExceptionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ParameterService {

    private final ParameterRepository parameterRepository;
    private final ParameterMapper parameterMapper;
    private final ParameterOptionRepository parameterOptionRepository;
    private final CategoryRepository categoryRepository;

    @CacheEvict(value = {"parameters", "parametersByCategory"}, allEntries = true)
    public ParameterResponseDto createParameter(ParameterRequestDto requestDto, String language) {
        log.info("Creating parameter for category ID: {}", requestDto.getCategoryId());

        String context = ExceptionHelper.createErrorContext(
                "createParameter", "Parameter", requestDto.getExternalId(),
                "category: " + requestDto.getCategoryId());

        return ExceptionHelper.wrapDatabaseOperation(() -> {
            validateParameterRequest(requestDto, true);

            Category category = findCategoryByIdOrThrow(requestDto.getCategoryId());

            checkForDuplicateParameter(requestDto, category);

            Parameter parameter = createParameterFromRequest(requestDto, category);
            parameter = parameterRepository.save(parameter);

            createParameterOptions(parameter, requestDto.getOptions());

            log.info("Parameter created successfully with id: {} and external id: {}",
                    parameter.getId(), parameter.getExternalId());

            return toResponseDto(parameter, language);

        }, context);
    }

    @CacheEvict(value = {"parameters", "parametersByCategory"}, allEntries = true)
    public ParameterResponseDto updateParameter(Long id, ParameterRequestDto requestDto, String language) {
        log.info("Updating parameter with ID: {}", id);

        String context = ExceptionHelper.createErrorContext("updateParameter", "Parameter", id, null);

        return ExceptionHelper.wrapDatabaseOperation(() -> {
            validateParameterId(id);
            validateParameterRequest(requestDto, false);

            Parameter existingParameter = findParameterByIdOrThrow(id);

            validateParameterNameUniqueness(requestDto, existingParameter);

            updateParameterFromRequest(existingParameter, requestDto);
            Parameter updatedParameter = parameterRepository.save(existingParameter);

            updateParameterOptions(updatedParameter, requestDto.getOptions());

            log.info("Parameter updated successfully with ID: {}", id);
            return toResponseDto(updatedParameter, language);

        }, context);
    }

    @CacheEvict(value = {"parameters", "parametersByCategory"}, allEntries = true)
    @Transactional
    public List<ParameterResponseDto> reorderParameters(Long categoryId, List<ParameterOrderDto> reorderDtos, String language) {
        if (reorderDtos == null || reorderDtos.isEmpty()) {
            throw new ValidationException("Reorder list cannot be empty.");
        }

        log.info("Reordering {} parameters for category ID: {}", reorderDtos.size(), categoryId);
        // validateCategoryId(categoryId); // Ако е необходимо да се провери категорията

        // 1. Извличаме всички параметри, които трябва да бъдат пренаредени
        Set<Long> parameterIds = reorderDtos.stream()
                .map(ParameterOrderDto::getParameterId)
                .collect(Collectors.toSet());

        List<Parameter> parameters = parameterRepository.findAllById(parameterIds);
        if (parameters.size() != parameterIds.size()) {
            // Проверка за невалидни ID-та
            String missingIds = parameterIds.stream()
                    .filter(id -> parameters.stream().noneMatch(p -> p.getId().equals(id)))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new ValidationException("One or more parameter IDs are invalid: " + missingIds);
        }

        // 2. Индексираме DTO-тата по ID за бърз достъп до новия 'order'
        Map<Long, Integer> newOrderMap = reorderDtos.stream()
                .collect(Collectors.toMap(ParameterOrderDto::getParameterId, ParameterOrderDto::getNewOrder));

        // 3. Обновяваме 'order' полето
        for (Parameter parameter : parameters) {
            Integer newOrder = newOrderMap.get(parameter.getId());

            // Допълнителна проверка: Уверете се, че параметърът е асоцииран с дадената категория
            boolean isInCategory = parameter.getCategories().stream()
                    .anyMatch(c -> c.getId().equals(categoryId));

            if (!isInCategory) {
                log.warn("Parameter {} is not associated with category {}", parameter.getId(), categoryId);
                throw new ValidationException(String.format("Parameter %d is not part of category %d", parameter.getId(), categoryId));
            }

            parameter.setOrder(newOrder);
        }

        // 4. Записваме промените и изчистваме кеша
        parameterRepository.saveAll(parameters);

        log.info("Successfully reordered {} parameters for category {}", parameters.size(), categoryId);

        return Collections.emptyList();
    }

    @CacheEvict(value = {"parameters", "parametersByCategory"}, allEntries = true)
    public ParameterResponseDto changeParameterVisibilityAsFilter(Long id) {
        log.info("Updating parameter with ID: {}", id);

        String context = ExceptionHelper.createErrorContext("updateParameter", "Parameter", id, null);

        return ExceptionHelper.wrapDatabaseOperation(() -> {
            validateParameterId(id);

            Parameter existingParameter = findParameterByIdOrThrow(id);
            if (existingParameter.getIsFilter()) {
                existingParameter.setIsFilter(false);
            } else {
                existingParameter.setIsFilter(true);
            }
            Parameter updatedParameter = parameterRepository.save(existingParameter);

            log.info("Parameter updated successfully with ID: {}", id);
            return toResponseDto(updatedParameter, "bg");

        }, context);
    }

    @CacheEvict(value = {"parameters", "parametersByCategory"}, allEntries = true)
    public void deleteParameter(Long parameterId) {
        log.info("Deleting parameter with ID: {}", parameterId);

        String context = ExceptionHelper.createErrorContext("deleteParameter", "Parameter", parameterId, null);

        ExceptionHelper.wrapDatabaseOperation(() -> {
            validateParameterId(parameterId);

            Parameter parameter = findParameterByIdOrThrow(parameterId);

            validateParameterDeletion(parameter);

            parameterRepository.delete(parameter);

            log.info("Parameter deleted successfully with ID: {}", parameterId);
            return null;
        }, context);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "parameters", key = "'category_all_' + #categoryId + '_' + #language")
    public List<ParameterResponseDto> findByCategory(Long categoryId, String language) {
        log.debug("Fetching all parameters for category: {}", categoryId);

        validateCategoryId(categoryId);
        findCategoryByIdOrThrow(categoryId);

        return ExceptionHelper.wrapDatabaseOperation(() -> {
            // ✅ Използваме новия Many-to-Many метод
            List<Parameter> parameters = parameterRepository.findByCategoryIdOrderByOrderAsc(categoryId);

            parameters.forEach(parameter -> {
                List<ParameterOption> uniqueOptions =
                        parameterOptionRepository.findUniqueOptionsByParameter(parameter.getId());
                parameter.setOptions(new HashSet<>(uniqueOptions));
            });

            return parameters.stream()
                    .map(p -> toResponseDto(p, language))
                    .toList();
        }, "fetch all parameters for category: " + categoryId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "parameters", key = "#id + '_' + #language")
    public ParameterResponseDto getParameterById(Long id, String language) {
        log.debug("Fetching parameter with ID: {}", id);

        validateParameterId(id);

        Parameter parameter = findParameterByIdOrThrow(id);
        return toResponseDto(parameter, language);
    }

    @Transactional(readOnly = true)
    public List<ParameterResponseDto> getAllParameters(String language) {
        log.debug("Fetching all parameters for language: {}", language);

        validateLanguage(language);

        return ExceptionHelper.wrapDatabaseOperation(() ->
                        parameterRepository.findAll().stream()
                                .map(parameter -> toResponseDto(parameter, language))
                                .toList(),
                "fetch all parameters"
        );
    }

    @CacheEvict(value = {"parameters", "parametersByCategory"}, allEntries = true)
    public void deleteParameterOption(Long parameterId, Long optionId) {
        log.info("Deleting parameter option with ID: {} from parameter: {}", optionId, parameterId);

        String context = ExceptionHelper.createErrorContext(
                "deleteParameterOption", "ParameterOption", optionId,
                "parameterId: " + parameterId);

        ExceptionHelper.wrapDatabaseOperation(() -> {
            validateParameterId(parameterId);

            if (optionId == null || optionId <= 0) {
                throw new ValidationException("Parameter option ID must be a positive number");
            }

            // ✅ Провери дали параметърът съществува
            Parameter parameter = findParameterByIdOrThrow(parameterId);

            // ✅ Намери опцията
            ParameterOption option = parameterOptionRepository.findById(optionId)
                    .orElseThrow(() -> new ValidationException(
                            String.format("Parameter option with ID %d not found", optionId)));

            // ✅ Провери дали опцията принадлежи на този параметър
            if (!option.getParameter().getId().equals(parameterId)) {
                throw new ValidationException(
                        String.format("Parameter option %d does not belong to parameter %d",
                                optionId, parameterId));
            }

            // ✅ Провери дали опцията се използва от продукти
            long productUsages = option.getProductParameters() != null ?
                    option.getProductParameters().size() : 0;

            if (productUsages > 0) {
                throw new BusinessLogicException(
                        String.format("Cannot delete parameter option '%s' because it is used by %d products. " +
                                        "Please remove it from products first.",
                                getOptionDisplayName(option), productUsages));
            }

            // ✅ Изтрий опцията
            parameterOptionRepository.delete(option);

            log.info("Parameter option deleted successfully: ID {}, name: '{}'",
                    optionId, getOptionDisplayName(option));
            return null;
        }, context);
    }

    public Page<ParameterResponseDto> findAllAdminParameters(Pageable pageable, String lang) {
        return parameterRepository.findByCreatedByOrderByCreatedAtDesc("ADMIN", pageable)
                .map(p -> toResponseDto(p, lang));
    }

    // ✅ Helper method
    private String getOptionDisplayName(ParameterOption option) {
        if (StringUtils.hasText(option.getNameBg())) {
            return option.getNameBg();
        } else if (StringUtils.hasText(option.getNameEn())) {
            return option.getNameEn();
        } else {
            return "Option ID: " + option.getId();
        }
    }

    // ========== PRIVATE VALIDATION METHODS ==========

    private void validateParameterId(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("Parameter ID must be a positive number");
        }
    }

    private void validateCategoryId(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new ValidationException("Category ID must be a positive number");
        }
    }

    private void validateLanguage(String language) {
        if (!StringUtils.hasText(language)) {
            throw new ValidationException("Language is required");
        }

        if (!language.matches("^(en|bg)$")) {
            throw new ValidationException("Language must be 'en' or 'bg'");
        }
    }

    private void validateParameterRequest(ParameterRequestDto requestDto, boolean isCreate) {
        if (requestDto == null) {
            throw new ValidationException("Parameter request cannot be null");
        }

        if (requestDto.getCategoryId() == null) {
            throw new ValidationException("Category ID is required");
        }

        if (requestDto.getName() == null || requestDto.getName().isEmpty()) {
            throw new ValidationException("Parameter name is required");
        }

        validateParameterNames(requestDto.getName());

        if (requestDto.getOrder() != null && requestDto.getOrder() < 0) {
            throw new ValidationException("Parameter order cannot be negative");
        }

        if (requestDto.getOptions() != null && !requestDto.getOptions().isEmpty()) {
            validateParameterOptions(requestDto.getOptions());
        }
    }

    private void validateParameterNames(List<com.techstore.dto.external.NameDto> names) {
        boolean hasValidName = false;

        for (var name : names) {
            if (StringUtils.hasText(name.getText())) {
                hasValidName = true;

                if (name.getText().trim().length() > 255) {
                    throw new ValidationException(
                            String.format("Parameter name (%s) cannot exceed 255 characters",
                                    name.getLanguageCode()));
                }

                if (name.getText().trim().length() < 2) {
                    throw new ValidationException(
                            String.format("Parameter name (%s) must be at least 2 characters long",
                                    name.getLanguageCode()));
                }
            }
        }

        if (!hasValidName) {
            throw new ValidationException("At least one parameter name (EN or BG) must be provided");
        }
    }

    private void validateParameterOptions(List<ParameterOptionRequestDto> options) {
        if (options.isEmpty()) {
            return;
        }

        Set<Long> externalIds = new HashSet<>();

        for (ParameterOptionRequestDto option : options) {
            // ✅ Проверка за external_id дубликати
            if (option.getExternalId() != null) {
                if (externalIds.contains(option.getExternalId())) {
                    throw new ValidationException("Duplicate parameter option external ID: " + option.getExternalId());
                }
                externalIds.add(option.getExternalId());
            }

            // ✅ Проверка за отрицателен order
            if (option.getOrder() != null && option.getOrder() < 0) {
                throw new ValidationException("Parameter option order cannot be negative");
            }

            // ✅ Проверка за име
            if (option.getName() == null || option.getName().isEmpty()) {
                throw new ValidationException("Parameter option name is required");
            }

            validateParameterOptionNames(option.getName());
        }
    }

    private void validateParameterOptionNames(List<com.techstore.dto.external.NameDto> names) {
        boolean hasValidName = false;

        for (var name : names) {
            if (StringUtils.hasText(name.getText())) {
                hasValidName = true;

                if (name.getText().trim().length() < 1) {
                    throw new ValidationException(
                            String.format("Parameter option name (%s) cannot be empty",
                                    name.getLanguageCode()));
                }
            }
        }

        if (!hasValidName) {
            throw new ValidationException("At least one parameter option name (EN or BG) must be provided");
        }
    }

    private void validateParameterDeletion(Parameter parameter) {
        if (parameter.getOptions() != null) {
            long totalProductUsages = parameter.getOptions().stream()
                    .mapToLong(option -> option.getProductParameters() != null ?
                            option.getProductParameters().size() : 0)
                    .sum();

            if (totalProductUsages > 0) {
                throw new BusinessLogicException(
                        String.format("Cannot delete parameter '%s' because it is used by %d products. " +
                                        "Please remove the parameter from products first.",
                                getParameterDisplayName(parameter), totalProductUsages));
            }
        }

        // ✅ Провери дали параметърът се използва в множество категории
        if (parameter.getCategories() != null && parameter.getCategories().size() > 1) {
            throw new BusinessLogicException(
                    String.format("Cannot delete parameter '%s' because it is shared across %d categories. " +
                                    "Please remove it from all categories first.",
                            getParameterDisplayName(parameter), parameter.getCategories().size()));
        }
    }

    private void validateParameterNameUniqueness(ParameterRequestDto requestDto, Parameter existingParameter) {
        if (requestDto.getName() == null || requestDto.getName().isEmpty()) {
            return;
        }

        boolean nameChanged = false;

        for (var nameDto : requestDto.getName()) {
            String newName = nameDto.getText() != null ? nameDto.getText().trim() : null;
            String currentName = null;

            if ("bg".equalsIgnoreCase(nameDto.getLanguageCode())) {
                currentName = existingParameter.getNameBg();
            } else if ("en".equalsIgnoreCase(nameDto.getLanguageCode())) {
                currentName = existingParameter.getNameEn();
            }

            if (!Objects.equals(newName, currentName)) {
                nameChanged = true;
                break;
            }
        }

        if (nameChanged) {
            // ✅ Проверяваме във ВСИЧКИ категории на параметъра
            for (Category category : existingParameter.getCategories()) {
                checkForDuplicateParameterNames(requestDto, category, existingParameter.getId());
            }
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Parameter findParameterByIdOrThrow(Long id) {
        return ExceptionHelper.findOrThrow(
                parameterRepository.findById(id).orElse(null),
                "Parameter",
                id
        );
    }

    private Category findCategoryByIdOrThrow(Long categoryId) {
        return ExceptionHelper.findOrThrow(
                categoryRepository.findById(categoryId).orElse(null),
                "Category",
                categoryId
        );
    }

    private void checkForDuplicateParameter(ParameterRequestDto requestDto, Category category) {
        checkForDuplicateParameterNames(requestDto, category, null);
    }

    private void checkForDuplicateParameterNames(ParameterRequestDto requestDto, Category category, Long excludeParameterId) {
        for (var nameDto : requestDto.getName()) {
            if (!StringUtils.hasText(nameDto.getText())) {
                continue;
            }

            String parameterName = nameDto.getText().trim();

            if ("bg".equalsIgnoreCase(nameDto.getLanguageCode())) {
                // ✅ Търсим параметри в тази категория с това име
                List<Parameter> duplicates = parameterRepository.findByCategoryIdOrderByOrderAsc(category.getId())
                        .stream()
                        .filter(p -> parameterName.equalsIgnoreCase(p.getNameBg()))
                        .filter(p -> excludeParameterId == null || !p.getId().equals(excludeParameterId))
                        .toList();

                if (!duplicates.isEmpty()) {
                    throw new DuplicateResourceException(
                            String.format("Parameter with Bulgarian name '%s' already exists in this category",
                                    parameterName));
                }
            } else if ("en".equalsIgnoreCase(nameDto.getLanguageCode())) {
                List<Parameter> duplicates = parameterRepository.findByCategoryIdOrderByOrderAsc(category.getId())
                        .stream()
                        .filter(p -> parameterName.equalsIgnoreCase(p.getNameEn()))
                        .filter(p -> excludeParameterId == null || !p.getId().equals(excludeParameterId))
                        .toList();

                if (!duplicates.isEmpty()) {
                    throw new DuplicateResourceException(
                            String.format("Parameter with English name '%s' already exists in this category",
                                    parameterName));
                }
            }
        }
    }

    private Parameter createParameterFromRequest(ParameterRequestDto requestDto, Category category) {
        Parameter parameter = new Parameter();
        parameter.setExternalId(requestDto.getExternalId());

        // ✅ Инициализираме categories Set и добавяме категорията
        parameter.setCategories(new HashSet<>());
        parameter.getCategories().add(category);

        parameter.setOrder(requestDto.getOrder() != null ? requestDto.getOrder() : 0);

        setParameterNamesFromRequest(parameter, requestDto.getName());
        parameter.setCreatedBy("ADMIN");
        parameter.setLastModifiedBy("ADMIN");

        return parameter;
    }

    private void updateParameterFromRequest(Parameter parameter, ParameterRequestDto requestDto) {
        parameter.setLastModifiedBy("ADMIN");
        if (requestDto.getOrder() != null) {
            parameter.setOrder(requestDto.getOrder());
        }

        if (requestDto.getName() != null && !requestDto.getName().isEmpty()) {
            setParameterNamesFromRequest(parameter, requestDto.getName());
        }

        // ✅ Ако се предава категория, добавяме я към Set-а
        if (requestDto.getCategoryId() != null) {
            Category category = findCategoryByIdOrThrow(requestDto.getCategoryId());
            if (!parameter.getCategories().contains(category)) {
                parameter.getCategories().add(category);
                log.info("Added category {} to parameter {}", category.getId(), parameter.getId());
            }
        }
    }

    private void setParameterNamesFromRequest(Parameter parameter, List<com.techstore.dto.external.NameDto> names) {
        for (var nameDto : names) {
            if (StringUtils.hasText(nameDto.getText())) {
                if ("bg".equalsIgnoreCase(nameDto.getLanguageCode())) {
                    parameter.setNameBg(nameDto.getText().trim());
                } else if ("en".equalsIgnoreCase(nameDto.getLanguageCode())) {
                    parameter.setNameEn(nameDto.getText().trim());
                }
            }
        }
    }

    private void createParameterOptions(Parameter parameter, List<ParameterOptionRequestDto> optionDtos) {
        if (optionDtos == null || optionDtos.isEmpty()) {
            return;
        }

        List<ParameterOption> options = new ArrayList<>();

        for (ParameterOptionRequestDto optionDto : optionDtos) {
            String optionNameBg = getOptionNameBg(optionDto);
            if (optionNameBg == null || optionNameBg.isEmpty()) {
                log.warn("Skipping option without Bulgarian name for parameter {}", parameter.getId());
                continue;
            }

            ParameterOption option = createParameterOptionFromRequest(optionDto, parameter);
            options.add(option);
            log.debug("Creating new option '{}' for new parameter", optionNameBg);
        }

        if (!options.isEmpty()) {
            parameterOptionRepository.saveAll(options);
            log.info("Created {} parameter options for new parameter: {}", options.size(), parameter.getId());
        }
    }

    private void updateParameterOptions(Parameter parameter, List<ParameterOptionRequestDto> optionDtos) {
        if (optionDtos == null) {
            return;
        }

        // ✅ 1. Зареди съществуващите options
        List<ParameterOption> existingOptions = parameterOptionRepository
                .findByParameterIdOrderByOrderAsc(parameter.getId());

        // ✅ 2. Индексирай по ID (за admin panel)
        Map<Long, ParameterOption> existingOptionsById = existingOptions.stream()
                .filter(opt -> opt.getId() != null)
                .collect(Collectors.toMap(
                        ParameterOption::getId,
                        option -> option
                ));

        // ✅ 3. Индексирай по externalId (за API sync)
        Map<Long, ParameterOption> existingOptionsByExternalId = existingOptions.stream()
                .filter(opt -> opt.getExternalId() != null)
                .collect(Collectors.toMap(
                        ParameterOption::getExternalId,
                        option -> option,
                        (option1, option2) -> {
                            log.warn("Duplicate external_id '{}' found for parameter {}, keeping first",
                                    option1.getExternalId(), parameter.getId());
                            return option1;
                        }
                ));

        List<ParameterOption> optionsToSave = new ArrayList<>();
        Set<Long> processedIds = new HashSet<>();

        // ✅ 4. Обработи options от request-а
        for (ParameterOptionRequestDto optionDto : optionDtos) {
            String optionNameBg = getOptionNameBg(optionDto);
            if (optionNameBg == null || optionNameBg.isEmpty()) {
                log.warn("Skipping option without Bulgarian name for parameter {}", parameter.getId());
                continue;
            }

            ParameterOption existingOption = null;

            // ✅ 4a. ПРИОРИТЕТ 1: Търси по database ID (от admin panel)
            if (optionDto.getOptionId() != null) {
                existingOption = existingOptionsById.get(optionDto.getOptionId());

                if (existingOption != null) {
                    // ✅ Валидация: провери дали option принадлежи на този параметър
                    if (!existingOption.getParameter().getId().equals(parameter.getId())) {
                        throw new ValidationException(
                                String.format("Parameter option %d does not belong to parameter %d",
                                        optionDto.getOptionId(), parameter.getId()));
                    }

                    processedIds.add(existingOption.getId());
                    log.debug("Found existing option by database ID: {} for parameter {}",
                            optionDto.getOptionId(), parameter.getId());
                } else {
                    throw new ValidationException(
                            String.format("Parameter option with ID %d not found", optionDto.getOptionId()));
                }
            }
            // ✅ 4b. ПРИОРИТЕТ 2: Търси по externalId (от API sync)
            else if (optionDto.getExternalId() != null) {
                existingOption = existingOptionsByExternalId.get(optionDto.getExternalId());

                if (existingOption != null) {
                    processedIds.add(existingOption.getId());
                    log.debug("Found existing option by external_id: {} for parameter {}",
                            optionDto.getExternalId(), parameter.getId());
                }
            }

            // ✅ 5. UPDATE съществуваща или CREATE нова опция
            if (existingOption != null) {
                // UPDATE съществуваща
                updateParameterOptionFromRequest(existingOption, optionDto);
                optionsToSave.add(existingOption);
                log.debug("Updating existing option '{}' for parameter {}",
                        getOptionDisplayName(existingOption), parameter.getId());
            } else {
                // CREATE нова опция
                ParameterOption newOption = createParameterOptionFromRequest(optionDto, parameter);
                optionsToSave.add(newOption);
                log.debug("Adding new option '{}' to parameter {}", optionNameBg, parameter.getId());
            }
        }

        // ✅ 6. DELETE options that are NO LONGER in the request
        List<ParameterOption> optionsToDelete = existingOptions.stream()
                .filter(option -> !processedIds.contains(option.getId()))
                .toList();

        if (!optionsToDelete.isEmpty()) {
            List<ParameterOption> safeToDelete = new ArrayList<>();

            for (ParameterOption optionToDelete : optionsToDelete) {
                long productUsages = optionToDelete.getProductParameters() != null ?
                        optionToDelete.getProductParameters().size() : 0;

                if (productUsages > 0) {
                    log.warn("Skipping deletion of parameter option '{}' (ID: {}) because it is used by {} products",
                            getOptionDisplayName(optionToDelete), optionToDelete.getId(), productUsages);
                } else {
                    safeToDelete.add(optionToDelete);
                }
            }

            if (!safeToDelete.isEmpty()) {
                parameterOptionRepository.deleteAll(safeToDelete);
                log.info("Deleted {} unused parameter options for parameter: {}",
                        safeToDelete.size(), parameter.getId());
            }
        }

        // ✅ 7. SAVE всички промени (updates + новите)
        if (!optionsToSave.isEmpty()) {
            parameterOptionRepository.saveAll(optionsToSave);
            log.info("Saved {} parameter options for parameter: {} (updates + additions)",
                    optionsToSave.size(), parameter.getId());
        }
    }

    private String getOptionNameBg(ParameterOptionRequestDto optionDto) {
        if (optionDto.getName() == null) return null;

        return optionDto.getName().stream()
                .filter(name -> "bg".equalsIgnoreCase(name.getLanguageCode()))
                .map(com.techstore.dto.external.NameDto::getText)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private ParameterOption createParameterOptionFromRequest(ParameterOptionRequestDto optionDto, Parameter parameter) {
        ParameterOption option = new ParameterOption();
        option.setExternalId(optionDto.getExternalId());
        option.setParameter(parameter);
        option.setOrder(optionDto.getOrder() != null ? optionDto.getOrder() : 0);

        setParameterOptionNamesFromRequest(option, optionDto.getName());
        option.setCreatedBy("ADMIN");

        return option;
    }

    private void updateParameterOptionFromRequest(ParameterOption option, ParameterOptionRequestDto optionDto) {
        if (optionDto.getOrder() != null) {
            option.setOrder(optionDto.getOrder());
        }

        if (optionDto.getName() != null && !optionDto.getName().isEmpty()) {
            setParameterOptionNamesFromRequest(option, optionDto.getName());
        }
    }

    private void setParameterOptionNamesFromRequest(ParameterOption option, List<com.techstore.dto.external.NameDto> names) {
        for (var nameDto : names) {
            if (StringUtils.hasText(nameDto.getText())) {
                if ("bg".equalsIgnoreCase(nameDto.getLanguageCode())) {
                    option.setNameBg(nameDto.getText().trim());
                } else if ("en".equalsIgnoreCase(nameDto.getLanguageCode())) {
                    option.setNameEn(nameDto.getText().trim());
                }
            }
        }
    }

    private String getParameterDisplayName(Parameter parameter) {
        if (StringUtils.hasText(parameter.getNameEn())) {
            return parameter.getNameEn();
        } else if (StringUtils.hasText(parameter.getNameBg())) {
            return parameter.getNameBg();
        } else {
            return "Parameter ID: " + parameter.getId();
        }
    }

    @Transactional(readOnly = true)
    public ParameterResponseDto toResponseDto(Parameter parameter, String language) {
        if (parameter.getCategories() != null) {
            parameter.getCategories().size();
        }
        return parameterMapper.toResponseDto(parameter, language);
    }
}