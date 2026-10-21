package com.vionsys.hireai.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModuleStatDto {
    @JsonAlias({"id", "module"})
    private String id;

    @JsonAlias({"name", "moduleName"})
    private String name;

    @JsonAlias({"module", "id"})
    private String module;

    @JsonAlias({"moduleName", "name"})
    private String moduleName;

    @JsonAlias({"calls", "callCount"})
    private Long calls;

    @JsonAlias({"callCount", "calls"})
    private Long callCount;

    private Double percentage;

    @JsonAlias({"avgLatencySeconds", "avg_latency_seconds"})
    private Double avgLatencySeconds;

    @JsonAlias({"failedCount", "failed_count"})
    private Long failedCount;

    public String getId() {
        return id != null ? id : module;
    }

    public String getModule() {
        return module != null ? module : id;
    }

    public String getName() {
        return name != null ? name : moduleName;
    }

    public String getModuleName() {
        return moduleName != null ? moduleName : name;
    }

    public Long getCalls() {
        return calls != null ? calls : callCount;
    }

    public Long getCallCount() {
        return callCount != null ? callCount : calls;
    }
}
