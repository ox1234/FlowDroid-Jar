package core;

import soot.jimple.infoflow.InfoflowConfiguration;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class InfoflowJarConfiguration extends InfoflowConfiguration {
    private String jarDir;
    private List<String> jarFiles = new LinkedList<>();
    private String sourceSinkFile;
    private String outPath;
    private List<String> entryClasses = new LinkedList<>();

    public String getJarDir() {
        return jarDir;
    }

    public void setJarDir(String jarDir) {
        this.jarDir = jarDir;
        File file = new File(jarDir);
        if(file.exists()){
            File[] jars = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            });
            assert jars != null;
            for(File jar : jars){
                jarFiles.add(jar.getAbsolutePath());
            }
        }
    }

    public List<String> getJarFiles() {
        return jarFiles;
    }

    public void setJarFiles(List<String> jarFiles) {
        this.jarFiles = jarFiles;
    }

    public String getOutPath() {
        return outPath;
    }

    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }

    public String getSourceSinkFile() {
        return sourceSinkFile;
    }

    public void setSourceSinkFile(String sourceSinkFile) {
        this.sourceSinkFile = sourceSinkFile;
    }

    public List<String> getEntryClasses() {
        return entryClasses;
    }

    public void setEntryClasses(String entryClasses) {
        this.entryClasses.addAll(Arrays.asList(entryClasses.split(",")));
    }
}
