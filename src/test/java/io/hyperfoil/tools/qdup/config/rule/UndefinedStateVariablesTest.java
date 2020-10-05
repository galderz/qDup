package io.hyperfoil.tools.qdup.config.rule;

import io.hyperfoil.tools.qdup.SshTestBase;
import io.hyperfoil.tools.qdup.config.CmdBuilder;
import io.hyperfoil.tools.qdup.config.RunConfig;
import io.hyperfoil.tools.qdup.config.RunConfigBuilder;
import io.hyperfoil.tools.qdup.config.RunSummary;
import io.hyperfoil.tools.qdup.config.yaml.Parser;
import org.junit.Test;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class UndefinedStateVariablesTest extends SshTestBase {

    @Test
    public void value_from_foreach(){
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = new RunConfigBuilder(CmdBuilder.getBuilder());
        builder.loadYaml(parser.loadFile("signal",stream(""+
                "scripts:",
                "  test:",
                "    - for-each:",
                "        name: FOO",
                "        input: [{ name: \"hibernate\", pattern: \"hibernate-core*jar\" }, { name: \"logging\", pattern: \"jboss-logging*jar\" }]",
                "      then:",
                "      - set-state: RUN.BAR ${{RUN.BAR:}}-${{FOO.name}}",
                "hosts:",
                "  local: me@localhost",
                "roles:",
                "  role:",
                "    hosts: [local]",
                "    run-scripts:",
                "    - test:",
                "        with:",
                "          from_with: in_with",
                "states:",
                "  from_state: in_state"
        ),false));
        RunConfig config = builder.buildConfig(parser);
        RunSummary summary = new RunSummary();
        UndefinedStateVariables rule = new UndefinedStateVariables(parser);
        summary.addRule("state",rule);
        summary.scan(config.getRoles(),builder);
        assertFalse("unexpected errors:\n"+summary.getErrors().stream().map(Objects::toString).collect(Collectors.joining("\n")),summary.hasErrors());
    }

    @Test
    public void error_set_after_used_separate_phase(){
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = new RunConfigBuilder(CmdBuilder.getBuilder());
        builder.loadYaml(parser.loadFile("signal",stream(""+
                "scripts:",
                "  set:",
                "    - set-state: later_phase wrong_phase",
                "  use:",
                "    - sh: ${{later_phase}}",
                "hosts:",
                "  local: me@localhost",
                "roles:",
                "  role:",
                "    hosts: [local]",
                "    run-scripts:",
                "    - use",
                "    cleanup-scripts:",
                "    - set"
        ),false));
        RunConfig config = builder.buildConfig(parser);
        RunSummary summary = new RunSummary();
        UndefinedStateVariables rule = new UndefinedStateVariables(parser);
        summary.addRule("state",rule);
        summary.scan(config.getRoles(),builder);
        assertTrue("expected errors:\n"+summary.getErrors().stream().map(Objects::toString).collect(Collectors.joining("\n")),summary.hasErrors());
    }

    @Test
    public void error_set_after_used_sequential_phase(){
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = new RunConfigBuilder(CmdBuilder.getBuilder());
        builder.loadYaml(parser.loadFile("signal",stream(""+
                "scripts:",
                "  set:",
                "    - set-state: later_phase wrong_phase",
                "  use:",
                "    - sh: ${{later_phase}}",
                "hosts:",
                "  local: me@localhost",
                "roles:",
                "  role:",
                "    hosts: [local]",
                "    setup-scripts:",
                "    - use",
                "    - set"
        ),false));
        RunConfig config = builder.buildConfig(parser);
        RunSummary summary = new RunSummary();
        UndefinedStateVariables rule = new UndefinedStateVariables(parser);
        summary.addRule("state",rule);
        summary.scan(config.getRoles(),builder);
        assertTrue("expected errors:\n"+summary.getErrors().stream().map(Objects::toString).collect(Collectors.joining("\n")),summary.hasErrors());
        assertEquals("unexpected number of variables: "+rule.getUsedVariables(),1,rule.getUsedVariables().size());

    }
    @Test
    public void error_missing_referenced_state_value() {
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = getBuilder();
        builder.loadYaml(parser.loadFile("ctrlC", stream("" +
                "scripts:",
                "  foo:",
                "    - sh: echo HI",
                "      then:",
                "      - sh: echo ${{BAR}}",
                "hosts:",
                "  local: "+getHost(),
                "roles:",
                "  doit:",
                "    hosts: [local]",
                "    setup-scripts: [foo]",
                "states:",
                "  BAR: ${{BIZ}}",
                "  BIZx: "
        ), false));
        RunConfig config = builder.buildConfig(parser);

        RunSummary summary = new RunSummary();
        UndefinedStateVariables rule = new UndefinedStateVariables(parser);
        summary.addRule("state",rule);
        summary.scan(config.getRoles(),builder);

        assertEquals("unexpected number of variables: "+rule.getUsedVariables(),2,rule.getUsedVariables().size());

        assertTrue("expected errors:\n"+summary.getErrors().stream().map(Objects::toString).collect(Collectors.joining("\n")),summary.hasErrors());

        assertTrue("runConfig errors:\n" + config.getErrorStrings().stream().collect(Collectors.joining("\n")), config.hasErrors());
        assertEquals("expect 1 errors:\n"+ config.getErrorStrings().stream().collect(Collectors.joining("\n")),1,config.getErrors().size());
    }

    @Test
    public void error_set_after_used_same_script(){
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = new RunConfigBuilder(CmdBuilder.getBuilder());
        builder.loadYaml(parser.loadFile("signal",stream(""+
            "scripts:",
            "  test:",
            "    - sh: ${{explicit}}",
            "    - set-state: explicit in_script",
            "hosts:",
            "  local: me@localhost",
            "roles:",
            "  role:",
            "    hosts: [local]",
            "    run-scripts:",
            "    - test"
        ),false));
        RunConfig config = builder.buildConfig(parser);
        RunSummary summary = new RunSummary();
        UndefinedStateVariables rule = new UndefinedStateVariables(parser);
        summary.addRule("state",rule);
        summary.scan(config.getRoles(),builder);
        assertTrue("expected errors:\n"+summary.getErrors().stream().map(Objects::toString).collect(Collectors.joining("\n")),summary.hasErrors());
        assertEquals("unexpected number of variables: "+rule.getUsedVariables(),1,rule.getUsedVariables().size());
    }

    @Test
    public void value_from_setstate(){
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = new RunConfigBuilder(CmdBuilder.getBuilder());
        builder.loadYaml(parser.loadFile("signal",stream(""+
                "scripts:",
                "  test:",
                "    - set-state: explicit in_script",
                "    - set-state: from_pattern ${{explicit}}",
                "    - set-state: from_pattern_default ${{missing:in_default}}",
                "    - set-state: from_pattern_with ${{from_with}}",
                "    - set-state: from_pattern_state ${{from_state}}",
                "    - sh: ${{explicit}}",
                "    - sh: ${{from_pattern}}",
                "    - sh: ${{from_pattern_default}}",
                "    - sh: ${{from_pattern_with}}",
                "    - sh: ${{from_pattern_state}}",
                "hosts:",
                "  local: me@localhost",
                "roles:",
                "  role:",
                "    hosts: [local]",
                "    run-scripts:",
                "    - test:",
                "        with:",
                "          from_with: in_with",
                "states:",
                "  from_state: in_state"
        ),false));
        RunConfig config = builder.buildConfig(parser);
        RunSummary summary = new RunSummary();
        UndefinedStateVariables rule = new UndefinedStateVariables(parser);
        summary.addRule("state",rule);
        summary.scan(config.getRoles(),builder);
        assertFalse("unexpected errors:\n"+summary.getErrors().stream().map(Objects::toString).collect(Collectors.joining("\n")),summary.hasErrors());
        assertEquals("unexpected number of variables: "+rule.getUsedVariables(),9,rule.getUsedVariables().size());
    }

    @Test
    public void value_from_regex(){
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = new RunConfigBuilder(CmdBuilder.getBuilder());
        builder.loadYaml(parser.loadFile("signal",stream(""+
                "scripts:",
                "  test:",
                "    - regex: (?<RUN.with_run_prefix>\\d+) (?<HOST.with_host_prefix>\\d+) (?<without_prefix>\\d+)",
                "    - sh: ${{with_run_prefix}}",
                "    - sh: ${{with_host_prefix}}",
                "    - sh: ${{without_prefix}}",
                "hosts:",
                "  local: me@localhost",
                "roles:",
                "  role:",
                "    hosts: [local]",
                "    run-scripts:",
                "    - test:"
        ),false));
        RunConfig config = builder.buildConfig(parser);
        RunSummary summary = new RunSummary();
        UndefinedStateVariables rule = new UndefinedStateVariables(parser);
        summary.addRule("state",rule);
        summary.scan(config.getRoles(),builder);
        assertFalse("unexpected errors:\n"+summary.getErrors().stream().map(Objects::toString).collect(Collectors.joining("\n")),summary.hasErrors());
        assertEquals("unexpected number of variables: "+rule.getUsedVariables(),3,rule.getUsedVariables().size());
    }

    @Test
    public void valid_patterns(){
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = new RunConfigBuilder(CmdBuilder.getBuilder());
        builder.loadYaml(parser.loadFile("signal",stream(""+
                "scripts:",
                "  test:",
                "    - log: ${{from_with}}",
                "    - sh: ${{from_state}}",
                "    - sh: ${{undefined_with_default:defaultValue}}",
                "    - sh: ${{undefined_with_empty_default:}}",
                "    - sh: ${{RUN.run_undefined_with_empty_default:}}",
                "    - sh: ${{stateJson.key}}",
                "    - sh: ${{withJson.key}}",
                "hosts:",
                "  local: me@localhost",
                "roles:",
                "  role:",
                "    hosts: [local]",
                "    run-scripts:",
                "    - test:",
                "        with: { from_with: alpha, withJson: { key: value } }",
                "states:",
                "  from_state: bravo",
                "  stateJson:",
                "    key: value"
        ),false));
        RunConfig config = builder.buildConfig(parser);
        RunSummary summary = new RunSummary();
        UndefinedStateVariables rule = new UndefinedStateVariables(parser);
        summary.addRule("state",rule);
        summary.scan(config.getRoles(),builder);

        assertFalse("unexpected errors:\n"+summary.getErrors().stream().map(Objects::toString).collect(Collectors.joining("\n")),summary.hasErrors());
        assertEquals("unexpected number of variables: "+rule.getUsedVariables(),7,rule.getUsedVariables().size());
    }
}
