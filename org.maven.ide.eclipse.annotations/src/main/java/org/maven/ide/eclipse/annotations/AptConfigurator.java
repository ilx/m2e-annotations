/**
 * (C) Ivica Loncar
 * License: Eclipse Public License
 */
package org.maven.ide.eclipse.annotations;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.metadata.ArtifactMetadata;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.apt.core.util.IFactoryPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Loncar
 */
public class AptConfigurator extends AbstractJavaProjectConfigurator {

    private final Logger log = LoggerFactory.getLogger(AptConfigurator.class);

    //    /**
    //     * @author Michael Glauche (http://glauche.de/)
    //     */
    //    @Override
    //    public AbstractBuildParticipant getBuildParticipant(final IMavenProjectFacade projectFacade, final MojoExecution execution, final IPluginExecutionMetadata executionMetadata) {
    //        return new AptBuildParticipant(execution);
    //    }

    /**
     * Configures maven project with associated annotation processors.
     *
     * Limitations:
     * <ul>
     *  <li>There can be only one outputDirectory.</li>
     *  <li>There can be only one configuration per project.</li>
     *  <li>Might not work for processors that must be run in batch mode.</li>
     * </ul>
     *
     * @author Ivica Loncar
     */
    @Override
    public void configure(final ProjectConfigurationRequest p_request, final IProgressMonitor p_monitor) throws CoreException {
        super.configure(p_request, p_monitor);

        IProject project = p_request.getProject();
        if (project.hasNature(JavaCore.NATURE_ID)) {
            // enable annotation processing
            IJavaProject javaProject = JavaCore.create(project);
            AptConfig.setEnabled(javaProject, true);

            // Associate jars containing annotation processors.
            ProcessorConfiguration processorConfiguration = getProcessorConfiguration(p_request, p_monitor);

            List<ArtifactRepository> remoteArtifactRepositories = p_request.getMavenProject().getRemoteArtifactRepositories();
            IMaven maven = MavenPlugin.getMaven();

            IFactoryPath factoryPath = AptConfig.getFactoryPath(javaProject);

            ArtifactMetadata[] artifactsMetadata = processorConfiguration.getProcessorArtifacts();
            for (ArtifactMetadata artifactMetadata : artifactsMetadata) {
                // Q: what about transitive dependencies?
                Artifact artifact = maven.resolve(artifactMetadata.getGroupId(), artifactMetadata.getArtifactId(), artifactMetadata.getVersion(), artifactMetadata.getType(),
                        artifactMetadata.getClassifier(), remoteArtifactRepositories, p_monitor);

                File file = artifact.getFile();
                if ((file == null) || !file.exists() || !file.canRead()) {
                    throw new IllegalStateException("Cannot find file for artifact " + artifact + " file:" + file);
                }

                factoryPath.addExternalJar(file);
            }

            AptConfig.setFactoryPath(javaProject, factoryPath);

            Map<String, String> optionMap = processorConfiguration.getOptionMap();
            // we would like to override existing files
            optionMap.put("defaultOverride", "true");
            AptConfig.setProcessorOptions(optionMap, javaProject);

            // output directory for generated sources
            AptConfig.setGenSrcDir(javaProject, processorConfiguration.getOutputDirectory());
            // From http://www.eclipse.org/forums/index.php?t=rview&goto=533747:
            //
            // Batch mode is mainly intended to support processors that can't handle incremental processing:
            // for example, that rely on static variables being properly initialized at the beginning of the run,
            // or that rely on being able to process all the files in a single round rather than one at a time,
            // or that make assumptions about how many rounds of processing will occur before  they exit.
            // Sun's JPA processors do, I think, need to be run in batch mode;
        }
    }

    private ProcessorConfiguration getProcessorConfiguration(final ProjectConfigurationRequest p_request, final IProgressMonitor p_monitor) throws CoreException {
        IMavenProjectFacade projectFacade = p_request.getMavenProjectFacade();
        MavenProject mavenProject = projectFacade.getMavenProject(p_monitor);

        Plugin queryDslPlugin = mavenProject.getPlugin("org.bsc.maven:maven-processor-plugin");
        ProcessorConfiguration processorConfiguration = new ProcessorConfiguration(queryDslPlugin);

        return processorConfiguration;
    }

}