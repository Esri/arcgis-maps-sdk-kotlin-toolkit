# Developer Setup

### Package structure

### Creating new toolkit components

To add a new toolkit component, use the  "new component starter" script. It can be found at the top level of the repo.

<pre>
================================================================================
Usage: new-component-starter.sh -n component-name

Description: generates a new toolkit component and microapp with the given name
 -n <name> the name of the new toolkit component
 -h        this help message
 ./new-component-starter.sh -n FloorFilter
================================================================================
</pre>

This script will 
* copy the `template` toolkit component module, and the `TemplateApp` microapp into new modules with the name provided to the script.
* update `settings.gradle.kts` to add these two new modules.
