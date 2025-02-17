package io.kestra.core.models.script;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.runners.RunContext;
import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.*;

/**
 * Base class for all script runners.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type", visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY)
@SuperBuilder(toBuilder = true)
@Getter
@NoArgsConstructor
@Introspected
public abstract class ScriptRunner {
    @NotBlank
    @Pattern(regexp="\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*")
    protected String type;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private transient Map<String, Object> additionalVars;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private transient Map<String, String> env;

    /**
     * This method will be called by the script plugin to run a script on a script runner.
     * Script runners may be local or remote.
     * For local script runner (like in process or in a local Docker engine), <code>filesToUpload</code> and <code>filesToDownload</code> may be ignored as they car directoy used the task working directory.
     * For remote script runner (like Kubernetes or in a cloud provider), <code>filesToUpload</code> must be used to upload input and namespace files to the runner,
     * and <code>filesToDownload</code> must be used to download output files from the runner.
     */
    public abstract RunnerResult run(RunContext runContext, ScriptCommands scriptCommands, List<String> filesToUpload, List<String> filesToDownload) throws Exception;

    public Map<String, Object> additionalVars(RunContext runContext, ScriptCommands scriptCommands) throws IllegalVariableEvaluationException {
        if (this.additionalVars == null) {
            this.additionalVars = new HashMap<>();

            if (scriptCommands.getAdditionalVars() != null) {
                this.additionalVars.putAll(scriptCommands.getAdditionalVars());
            }

            this.additionalVars.putAll(this.runnerAdditionalVars(runContext, scriptCommands));
        }

        return this.additionalVars;
    }

    protected Map<String, Object> runnerAdditionalVars(RunContext runContext, ScriptCommands scriptCommands) throws IllegalVariableEvaluationException {
        return new HashMap<>();
    }

    public Map<String, String> env(RunContext runContext, ScriptCommands scriptCommands) throws IllegalVariableEvaluationException {
        if (this.env == null) {
            this.env = new HashMap<>();

            if (scriptCommands.getEnv() != null) {
                this.env.putAll(scriptCommands.getEnv());
            }

            Map<String, Object> additionalVars = this.additionalVars(runContext, scriptCommands);

            if (additionalVars.containsKey(ScriptService.VAR_WORKING_DIR)) {
                this.env.put(ScriptService.ENV_WORKING_DIR, additionalVars.get(ScriptService.VAR_WORKING_DIR).toString());
            }
            if (additionalVars.containsKey(ScriptService.VAR_OUTPUT_DIR)) {
                this.env.put(ScriptService.ENV_OUTPUT_DIR, additionalVars.get(ScriptService.VAR_OUTPUT_DIR).toString());
            }
            if (additionalVars.containsKey(ScriptService.VAR_BUCKET_PATH)) {
                this.env.put(ScriptService.ENV_BUCKET_PATH, additionalVars.get(ScriptService.VAR_BUCKET_PATH).toString());
            }

            this.env.putAll(this.runnerEnv(runContext, scriptCommands));
        }

        return this.env;
    }

    protected Map<String, String> runnerEnv(RunContext runContext, ScriptCommands scriptCommands) throws IllegalVariableEvaluationException {
        return new HashMap<>();
    }
}
