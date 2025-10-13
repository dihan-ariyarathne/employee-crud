package com.acme.employee.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.acme.employee.dto.EmployeePatchRequest;
import com.acme.employee.dto.EmployeeQueryParams;
import com.acme.employee.dto.EmployeeResponse;
import com.acme.employee.dto.EmployeeUpsertRequest;
import com.acme.employee.dto.PageResponse;
import com.acme.employee.service.EmployeeService;

import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private static final Set<String> RESERVED_PARAMS = Set.of("page", "size", "sort", "direction", "search");

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public Mono<PageResponse<EmployeeResponse>> list(@RequestParam(name = "page", defaultValue = "0") int page,
                                                     @RequestParam(name = "size", defaultValue = "20") int size,
                                                     @RequestParam(name = "sort", required = false) String sort,
                                                     @RequestParam(name = "direction", required = false) String direction,
                                                     @RequestParam(name = "search", required = false) String search,
                                                     ServerWebExchange exchange) {
        Map<String, String> filters = extractFilters(exchange.getRequest().getQueryParams());
        EmployeeQueryParams params = new EmployeeQueryParams(page, size, sort, direction, search, filters);
        return employeeService.list(params);
    }

    @GetMapping("/{id}")
    public Mono<EmployeeResponse> get(@PathVariable(name = "id") String id) {
        return employeeService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<EmployeeResponse> create(@RequestBody @Valid EmployeeUpsertRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/{id}")
    public Mono<EmployeeResponse> replace(@PathVariable(name = "id") String id,
                                          @RequestBody @Valid EmployeeUpsertRequest request) {
        return employeeService.replace(id, request);
    }

    @PostMapping("/{id}")
    public Mono<EmployeeResponse> upsert(@PathVariable(name = "id") String id,
                                         @RequestBody @Valid EmployeeUpsertRequest request) {
        return employeeService.replace(id, request);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}")
    public Mono<EmployeeResponse> patch(@PathVariable(name = "id") String id,
                                        @RequestBody @Valid EmployeePatchRequest request) {
        return employeeService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable(name = "id") String id,
                             @RequestParam(name = "soft", defaultValue = "true") boolean softDelete) {
        return employeeService.delete(id, softDelete);
    }

    private Map<String, String> extractFilters(MultiValueMap<String, String> queryParams) {
        Map<String, String> filters = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            if (RESERVED_PARAMS.contains(entry.getKey())) {
                continue;
            }
            if (entry.getValue().isEmpty()) {
                continue;
            }
            filters.put(entry.getKey(), entry.getValue().getFirst());
        }
        return filters;
    }
}
