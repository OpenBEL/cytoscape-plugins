# vim: filetype=ruby:ts=2:sw=2 :
require 'fileutils'
require 'pathname'
require 'set'

repositories.remote << 'http://repo1.maven.org/maven2'
repositories.remote << 'http://code.cytoscape.org/nexus/content/repositories/public'

# build settings
OPENBEL_KAM_NAVIGATOR_PLUGIN = 'OpenBEL - Cytoscape Navigator'
PLUGIN_VERSION = '1.0'
settings = Buildr.settings.build
settings['junit'] = '4.11'

# aliases
task :c  => [:clean]
task :p  => [:package]
task :cp => [:clean, :package]

# common files
MANIFEST = 'resources/META-INF/MANIFEST.MF'

# dependency versions
CY_API_VERSION = '3.0.0'
OSGI_VERSION = '4.2.0'

# dependencies
CY_APPLICATION_API = 'org.cytoscape:application-api:jar:' + CY_API_VERSION
CY_MODEL_API = 'org.cytoscape:model-api:jar:' + CY_API_VERSION
CY_SESSION_API = 'org.cytoscape:session-api:jar:' + CY_API_VERSION
CY_WORK_API = 'org.cytoscape:work-api:jar:' + CY_API_VERSION
CY_SERVICE_API = 'org.cytoscape:service-api:jar:' + CY_API_VERSION
CY_SWING_APPLICATION_API = 'org.cytoscape:swing-application-api:jar:' +
                           CY_API_VERSION
OSGI_CORE = 'org.osgi:org.osgi.core:jar:' + OSGI_VERSION
OPENBEL_WS_MODEL = 'org.openbel:org.openbel.framework.ws.model:jar:3.0.0'

# establish flat layout
layout = Layout.new
layout[:source, :main, :java] = 'src'
layout[:source, :test, :java] = 'test'
layout[:source, :main, :resources] = 'resources'
layout[:source, :test, :resources] = 'test'

# define the parent project
desc OPENBEL_KAM_NAVIGATOR_PLUGIN
define OPENBEL_KAM_NAVIGATOR_PLUGIN, :layout => layout do
  project.group = 'org.openbel'
  project.version = PLUGIN_VERSION
  default_compile_opts compile
  eclipse.options.short_names = true
  eclipse.natures 'org.eclipse.jdt.core.javanature'

  define 'org.openbel.cytoscape.navigator.api' do
    configure(project)
    compile.with CY_APPLICATION_API, CY_WORK_API, CY_SESSION_API,
                 CY_SERVICE_API, CY_MODEL_API
    package(:jar, :id => _id(project)).with(:manifest => file(MANIFEST))
  end

  define 'org.openbel.cytoscape.navigator.impl' do
    configure(project)
    compile.with projects('org.openbel.cytoscape.navigator.api'),
                 OSGI_CORE, CY_APPLICATION_API, CY_SESSION_API, CY_WORK_API,
                 CY_SERVICE_API, CY_MODEL_API
    package(:jar, :id => _id(project)).with(:manifest => file(MANIFEST))
  end
end

# Configures default compilation options (1.7 and all lint checks).
def default_compile_opts(compile)
  compile.options.source = '1.7'
  compile.options.target = '1.7'
  compile.options.lint = 'all'
  compile.options.other = %w{-encoding utf-8}
end

# Default project configuration.
def configure(project)
  project.version = PLUGIN_VERSION
end

def _id(project)
  return project.name.gsub(OPENBEL_KAM_NAVIGATOR_PLUGIN + ':', '')
end

