# m2e-annotations

m2e-annotations - eclipse plugin that configures m2e eclipse projects to use annotation processors via org.bsc.maven:maven-processor-plugin.

Accordingly to https://bugs.eclipse.org/bugs/attachment.cgi?id=199751 there will be built-in support for annotation processors, until that happens you can try this configurer.



## Limitations

There are some limitations:
- there can be only one configuration per maven project
- there can be only one output folder
- all of the dependencies have to be added to the dependencies section of configuration element

