package io.hyperfoil.tools.qdup.config.yaml;

import io.hyperfoil.tools.qdup.Globals;
import io.hyperfoil.tools.qdup.State;
import io.hyperfoil.tools.qdup.cmd.Script;
import io.hyperfoil.tools.qdup.config.Role;
import io.hyperfoil.tools.qdup.config.RunConfigBuilder;
import io.hyperfoil.tools.yaup.json.Json;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class YamlFile {

    private String path;
    private String name;
    private Map<String, Script> scripts;
    private Map<String, String> hosts;
    private State state;
    private Map<String, Role> roles;
    private Globals globals;

    public YamlFile(){
        name = "";
        scripts = new LinkedHashMap<>();
        hosts = new LinkedHashMap<>();
        state = new State(State.RUN_PREFIX);
        roles = new LinkedHashMap<>();
        globals = new Globals();
    }

    public void addHost(String name,String host){
        hosts.put(name,host);
    }
    public void addScript(String name,Script script){
        scripts.put(name,script);
    }
    public void addRole(String name,Role role){
        roles.putIfAbsent(name,role);
    }
    public void setName(String name){
        this.name = name;
    }
    public void setPath(String path){
        this.path = path;
        File f = new File(path);
        if(f.exists()){
            String parentPath = f.getAbsoluteFile().getParentFile().getPath();
            for(Script s : scripts.values()){
                s.with(RunConfigBuilder.SCRIPT_DIR,parentPath);
            }
        }
    }

    public String getName(){return name;}
    public String getPath(){return path;}
    public Map<String,String> getHosts(){
        return Collections.unmodifiableMap(hosts);
    }
    public Map<String,Script> getScripts(){
        return Collections.unmodifiableMap(scripts);
    }
    public Map<String,Role> getRoles(){
        return Collections.unmodifiableMap(roles);
    }
    public State getState(){
        return state;
    }
    public Globals getGlobals(){
        return globals;
    }

}
