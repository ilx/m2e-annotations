/**
 * (C) Ivica Loncar
 * License: Eclipse Public License
 */
package org.maven.ide.eclipse.annotations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.ArtifactScopeEnum;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.repository.metadata.ArtifactMetadata;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @author Ivica Loncar
 */
public class ProcessorConfiguration {
    private final Plugin                 m_plugin;
    private String                       m_outputDirectory;
    private final List<String>           m_annotationProcessors = new ArrayList<String>();
    private final List<ArtifactMetadata> m_processorArtifacts   = new ArrayList<ArtifactMetadata>();
    private final Map<String, String>    m_optionMap            = new LinkedHashMap<String, String>();

    public ProcessorConfiguration(final Plugin p_mavenPlugin) {
        m_plugin = p_mavenPlugin;
        // init configuration values
        configure();
    }

    public String getOutputDirectory() {
        return m_outputDirectory;
    }

    public String[] getProcessors() {
        return m_annotationProcessors.toArray(new String[0]);
    }

    public ArtifactMetadata[] getProcessorArtifacts() {
        return m_processorArtifacts.toArray(new ArtifactMetadata[0]);
    }

    public List<Dependency> getDependencies() {
        Set<String> deps = new LinkedHashSet<String>();
        return m_plugin.getDependencies();
    }

    public Map<String, String> getOptionMap() {
        return m_optionMap;
    }

    private void configure() {
        // aggregate plugin executions (can there be more than one?):
        List<PluginExecution> executions = m_plugin.getExecutions();

        for (PluginExecution pluginExecution : executions) {
            Xpp3Dom configuration = (Xpp3Dom) pluginExecution.getConfiguration();

            if (configuration == null) {
                throw new IllegalStateException(errorMsg("Plugin execution does not have configuration element."));
            }

            Xpp3Dom outputDirectoryElement = configuration.getChild("outputDirectory");
            if (outputDirectoryElement != null) {
                if (m_outputDirectory != null) {
                    // output directory already set, replacing it
                }
                m_outputDirectory = outputDirectoryElement.getValue();
            }

            Xpp3Dom processors = configuration.getChild("processors");
            if (processors != null) {
                Xpp3Dom[] processorElements = processors.getChildren("processor");

                if (processorElements == null) {
                    throw new IllegalStateException(errorMsg("There are no processors elements."));
                }

                for (Xpp3Dom processorElement : processorElements) {
                    if (processorElement != null) {
                        m_annotationProcessors.add(processorElement.getValue());
                    }
                }
            }

            Xpp3Dom optionMap = configuration.getChild("optionMap");
            if (optionMap != null) {
                Xpp3Dom[] keys = optionMap.getChildren();
                for (Xpp3Dom key : keys) {
                    String optionName = key.getName();
                    String optionValue = key.getValue();
                    m_optionMap.put(optionName, optionValue);
                }
            }
        }

        // dependencies = artifacts that contain annotation processors
        List<Dependency> dependencies = m_plugin.getDependencies();
        for (Dependency dependency : dependencies) {
            String scopeString = dependency.getScope();
            ArtifactScopeEnum scope = ArtifactScopeEnum.valueOf(scopeString);
            ArtifactMetadata artifactMetadata = new ArtifactMetadata(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getType(), scope,
                    dependency.getClassifier());
            m_processorArtifacts.add(artifactMetadata);
        }

    }

    private String errorMsg(final String p_msg) {
        String msg = String.format("Plugin %s:%s is not properly configured. %s", m_plugin.getGroupId(), m_plugin.getArtifactId(), p_msg);
        return msg;
    }

}