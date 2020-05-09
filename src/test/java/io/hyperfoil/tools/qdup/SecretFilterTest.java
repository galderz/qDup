package io.hyperfoil.tools.qdup;

import io.hyperfoil.tools.qdup.cmd.Dispatcher;
import io.hyperfoil.tools.qdup.cmd.SpyContext;
import io.hyperfoil.tools.qdup.cmd.impl.Regex;
import io.hyperfoil.tools.qdup.config.RunConfig;
import io.hyperfoil.tools.qdup.config.RunConfigBuilder;
import io.hyperfoil.tools.qdup.config.yaml.Parser;
import io.hyperfoil.tools.yaup.json.Json;
import org.junit.Test;

import static org.junit.Assert.*;

public class SecretFilterTest extends SshTestBase {


   @Test
   public void detect_secret_in_state(){
      State state = new State("");

      state.set(SecretFilter.SECRET_NAME_PREFIX+"foo","BAR");

      SecretFilter secretFilter = state.getSecretFilter();

      assertEquals("expect 1 secret",1,secretFilter.size());
      assertTrue("state should add key without prefix: "+state.getKeys(),state.has("foo"));
      assertFalse("state should not add key with prefix: "+state.getKeys(),state.allKeys().contains(SecretFilter.SECRET_NAME_PREFIX+"foo"));
      //assertEquals("get with prefix returns value","BAR",state.get(SecretFilter.SECRET_NAME_PREFIX+"foo"));
      assertEquals("get with prefix returns value","BAR",state.get("foo"));
   }

   @Test
   public void detect_secret_in_regex(){
      Regex regex = new Regex("(?<"+SecretFilter.SECRET_NAME_PREFIX+"foo>\\d+)");
      SpyContext spyContext = new SpyContext();
      regex.run("123abc",spyContext);
      State state = spyContext.getState();
      SecretFilter secretFilter = state.getSecretFilter();

      assertEquals("exect an entry in the filter",1,secretFilter.size());
      assertTrue("expect foo in state: "+state.allKeys(),state.allKeys().contains("foo"));
   }

   @Test
   public void check_log_for_secret_from_with_setState_states(){
      Parser parser = Parser.getInstance();
      RunConfigBuilder builder = getBuilder();
      builder.loadYaml(parser.loadFile("",stream(""+
         "scripts:",
         "  secrets:",
         "  - set-state: \""+SecretFilter.SECRET_NAME_PREFIX+"foo BAR\"",
         "  - sh: echo ${{foo}}__${{biz}}",
         "    with:",
         "      "+SecretFilter.SECRET_NAME_PREFIX+"biz: BAR",
         "  - set-state: RUN.output",
         "  - regex: (?<"+SecretFilter.SECRET_NAME_PREFIX+"RUN.match>.*)",
         "    then:",
         "    - sh: echo ${{match}}",
         "hosts:",
         "  local: " + getHost(),
         "roles:",
         "  doit:",
         "    hosts: [local]",
         "    setup-scripts: [secrets]",
         "states:",
         "  "+SecretFilter.SECRET_NAME_PREFIX+"FOO: BAR"
      ),false));
      RunConfig config = builder.buildConfig();

      Dispatcher dispatcher = new Dispatcher();
      Run doit = new Run(tmpDir.toString(), config, dispatcher);
      doit.run();

      Object output = doit.getConfig().getState().get("output");
      Object match = doit.getConfig().getState().get("match");
      assertNotNull("run state should contain output",output);
      assertNotNull("run state should contain match",match);
      assertEquals("output should be actual value of secret","BAR__BAR",output.toString());
      assertEquals("matches should be actual value of secret","BAR__BAR",match.toString());
      String runLogContents = readFile(tmpDir.getPath().resolve("run.log"));
      String runJsonContents = readFile(tmpDir.getPath().resolve("run.json"));
      assertFalse("log should not contain BAR\n"+runLogContents,runLogContents.contains("BAR"));
      assertFalse("run.json should not contain BAR\n"+runJsonContents,runLogContents.contains("BAR"));
      try {
         Json json = Json.fromString(runJsonContents);
         assertFalse("json should not be empty",json.isEmpty());
      }catch(RuntimeException e){
         fail("Exception creating json from run.js\n"+e.getMessage());
      }
   }
}
