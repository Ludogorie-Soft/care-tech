package com.techstore.controller;

import com.techstore.dto.filter.FilterConfigDto.AttributeDetail;
import com.techstore.dto.filter.FilterConfigDto.AttributeRequest;
import com.techstore.dto.filter.FilterConfigDto.AttributeSummary;
import com.techstore.dto.filter.FilterConfigDto.CategoryConfig;
import com.techstore.dto.filter.FilterConfigDto.CategorySummary;
import com.techstore.dto.filter.FilterConfigDto.ModeRequest;
import com.techstore.dto.filter.FilterConfigDto.NameRule;
import com.techstore.dto.filter.FilterConfigDto.NameRuleRequest;
import com.techstore.dto.filter.FilterConfigDto.OrderRequest;
import com.techstore.dto.filter.FilterConfigDto.PreviewRequest;
import com.techstore.dto.filter.FilterConfigDto.PreviewRow;
import com.techstore.dto.filter.FilterConfigDto.RawParameter;
import com.techstore.dto.filter.FilterConfigDto.Source;
import com.techstore.dto.filter.FilterConfigDto.SourceRequest;
import com.techstore.dto.filter.FilterConfigDto.UnmappedValue;
import com.techstore.dto.filter.FilterConfigDto.Value;
import com.techstore.dto.filter.FilterConfigDto.ValueRequest;
import com.techstore.dto.filter.FilterConfigDto.ValueRule;
import com.techstore.dto.filter.FilterConfigDto.ValueRuleRequest;
import com.techstore.service.filter.FilterConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Filter admin: attributes, values, mapping rules and the groups of each category. Rule changes reach
 * shoppers with the next rebuild ({@code POST /api/admin/filters/rebuild}); category groups at once.
 */
@RestController
@RequestMapping("/api/admin/filters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class FilterConfigController {

    private final FilterConfigService service;

    // ── Attributes and values ───────────────────────────────────────────────────────────────────────

    @GetMapping("/attributes")
    public List<AttributeSummary> attributes(@RequestParam(required = false) String q) {
        return service.listAttributes(q);
    }

    @GetMapping("/attributes/{id}")
    public AttributeDetail attribute(@PathVariable Long id) {
        return service.getAttribute(id);
    }

    @PostMapping("/attributes")
    public ResponseEntity<AttributeSummary> createAttribute(@RequestBody AttributeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAttribute(request));
    }

    @PutMapping("/attributes/{id}")
    public AttributeSummary updateAttribute(@PathVariable Long id, @RequestBody AttributeRequest request) {
        return service.updateAttribute(id, request);
    }

    @PostMapping("/attributes/{id}/values")
    public ResponseEntity<Value> createValue(@PathVariable Long id, @RequestBody ValueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createValue(id, request));
    }

    @PutMapping("/values/{valueId}")
    public Value updateValue(@PathVariable Long valueId, @RequestBody ValueRequest request) {
        return service.updateValue(valueId, request);
    }

    @DeleteMapping("/values/{valueId}")
    public ResponseEntity<Void> deleteValue(@PathVariable Long valueId) {
        service.deleteValue(valueId);
        return ResponseEntity.noContent().build();
    }

    // ── Rules ───────────────────────────────────────────────────────────────────────────────────────

    @PostMapping("/attributes/{id}/value-rules")
    public ResponseEntity<ValueRule> createValueRule(@PathVariable Long id, @RequestBody ValueRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createValueRule(id, request));
    }

    @DeleteMapping("/value-rules/{ruleId}")
    public ResponseEntity<Void> deleteValueRule(@PathVariable Long ruleId) {
        service.deleteValueRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/attributes/{id}/name-rules")
    public ResponseEntity<NameRule> createNameRule(@PathVariable Long id, @RequestBody NameRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createNameRule(id, request));
    }

    @DeleteMapping("/name-rules/{ruleId}")
    public ResponseEntity<Void> deleteNameRule(@PathVariable Long ruleId) {
        service.deleteNameRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    /** Supplier values of the attribute a pattern would match — checked before the rule is saved. */
    @PostMapping("/attributes/{id}/preview")
    public List<PreviewRow> preview(@PathVariable Long id, @RequestBody PreviewRequest request) {
        return service.preview(id, request.pattern());
    }

    // ── Supplier parameters ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/sources")
    public List<Source> sources(@RequestParam String name) {
        return service.sourcesFor(name);
    }

    @PostMapping("/sources")
    public ResponseEntity<Source> createSource(@RequestBody SourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSource(request));
    }

    @DeleteMapping("/sources/{sourceId}")
    public ResponseEntity<Void> deleteSource(@PathVariable Long sourceId) {
        service.deleteSource(sourceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unmapped")
    public List<UnmappedValue> unmapped(@RequestParam(required = false) Long attributeId,
                                        @RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String q) {
        return service.unmapped(attributeId, categoryId, q);
    }

    @GetMapping("/categories/{categoryId}/raw-parameters")
    public List<RawParameter> rawParameters(@PathVariable Long categoryId, @RequestParam(required = false) String q) {
        return service.rawParameters(categoryId, q);
    }

    // ── Categories ──────────────────────────────────────────────────────────────────────────────────

    @GetMapping("/categories")
    public List<CategorySummary> categories(@RequestParam(required = false) String q) {
        return service.categories(q);
    }

    @GetMapping("/categories/{categoryId}")
    public CategoryConfig category(@PathVariable Long categoryId) {
        return service.category(categoryId);
    }

    @PutMapping("/categories/{categoryId}/mode")
    public CategoryConfig setMode(@PathVariable Long categoryId, @RequestBody ModeRequest request) {
        return service.setMode(categoryId, request.mode());
    }

    @PostMapping("/categories/{categoryId}/attributes/{attributeId}")
    public CategoryConfig addGroup(@PathVariable Long categoryId, @PathVariable Long attributeId) {
        return service.addGroup(categoryId, attributeId);
    }

    @DeleteMapping("/categories/{categoryId}/attributes/{attributeId}")
    public CategoryConfig removeGroup(@PathVariable Long categoryId, @PathVariable Long attributeId) {
        return service.removeGroup(categoryId, attributeId);
    }

    @PutMapping("/categories/{categoryId}/order")
    public CategoryConfig reorder(@PathVariable Long categoryId, @RequestBody OrderRequest request) {
        return service.reorder(categoryId, request.attributeIds());
    }
}
