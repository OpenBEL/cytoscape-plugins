# OpenBEL KAM Navigator Plugin for Cytoscape
# vim: filetype=ruby:ts=2:sw=2 :
#
# set repositories
require 'fileutils'
require 'pathname'
require 'set'

repositories.remote << 'http://repo1.maven.org/maven2'
repositories.remote << 'http://code.cytoscape.org/nexus/content/repositories/public'

# build settings
OPENBEL_KAM_NAVIGATOR_PLUGIN = 'OpenBEL - KAM Navigator Plugin'
VERSION = '0.9'
settings = Buildr.settings.build
settings['junit'] = '4.11'

# aliases
task :c  => [:clean]
task :p  => [:package]
task :cy  => [:cytoscape]
task :cp => [:clean, :package]
task :cpy => [:cp, :cy]

# common files
MANIFEST = 'resources/META-INF/MANIFEST.MF'

# dependencies
CYTOSCAPE = 'cytoscape:application:jar:2.8.3'
DING = 'cytoscape.corelibs:ding:jar:2.8.3'
TASK = 'cytoscape.corelibs:task:jar:2.8.3'
OPENBEL_WS_MODEL = 'org.openbel:org.openbel.framework.ws.model:jar:3.0.0'
GINY = 'cytoscape.corelibs:giny:jar:2.8.3'
EQUATIONS = 'cytoscape.corelibs:equations:jar:2.8.3'

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
  project.version = VERSION
  default_compile_opts compile
  eclipse.options.short_names = true
  eclipse.natures 'org.eclipse.jdt.core.javanature'

  define 'org.openbel.cytoscape.webservice' do
    configure(project)
    compile.with CYTOSCAPE, OPENBEL_WS_MODEL

    # accumulate dependent projects + transitives
    jars = []
    cp_array = project.compile.classpath.to_a()
    jars << cp_array.map { |i| i.to_s }
    jars.flatten!.uniq!
    jars.delete_if { |i| i.include? 'application-2.8.3.jar' }

    # merge into one jar
    package(:jar, :id => _id(project)) \
      .with(:manifest => file(MANIFEST)) \
      .merge(jars)
  end

  define 'org.openbel.cytoscape.navigator' do
    configure(project)
    compile.with projects('org.openbel.cytoscape.webservice'), CYTOSCAPE,
                 DING, GINY, EQUATIONS, TASK, OPENBEL_WS_MODEL

    # accumulate dependent projects + transitives
    include_projects = projects('org.openbel.cytoscape.webservice')
    jars = []
    include_projects.each { |project|
      jars << Dir[project.path_to(:target) + '/*.jar']
      cp_array = project.compile.classpath.to_a()
      jars << cp_array.map { |i| i.to_s }
      jars.flatten!.uniq!
    }
    jars.delete_if { |i| i.include? 'application-2.8.3.jar' }

    # merge into one jar
    package(:jar, :id => _id(project)) \
      .with(:manifest => file(MANIFEST)) \
      .merge(jars)
  end
end

# Configures default compilation options (1.6 and all lint checks).
def default_compile_opts(compile)
  compile.options.source = '1.6'
  compile.options.target = '1.6'
  compile.options.lint = 'all'
  compile.options.other = %w{-encoding utf-8}
end

# Default project configuration.
def configure(project)
  project.version = VERSION
end

def _id(project)
  return project.name.gsub(OPENBEL_KAM_NAVIGATOR_PLUGIN + ':', '')
end

# extension - run cytoscape task
task :cytoscape_task do
  if not ENV['JAVA_HOME']
    raise "JAVA_HOME environment variable must be set."
  end
  if not ENV['CYTOSCAPE_HOME']
    raise "CYTOSCAPE_HOME environment variable must be set."
  end
  
  cyhome = ENV['CYTOSCAPE_HOME']
  plugins_dir = Pathname.new(cyhome) + 'plugins'
  puts projects
  prj = projects('OpenBEL - KAM Navigator Plugin:org.openbel.cytoscape.navigator')[0]
  plugin_jar = Dir[prj.path_to(:target) + '/*.jar'][0]

  puts "Copying #{plugin_jar} to #{plugins_dir}"
  FileUtils.copy(plugin_jar, plugins_dir)
  if ENV['JREBEL_HOME']
    jrebel = "-javaagent:#{Pathname.new(ENV['JREBEL_HOME']) + 'jrebel.jar'}"
    puts "Rebel forces enabled.  The force is strong with this one."
  else
    jrebel = ""
    puts "Rebel forces disabled.  Recharge batteries and try again."
  end
  puts "Launching Cytoscape using CYTOSCAPE_HOME: #{cyhome}"
  cy_command = %Q(java
    -Dswing.aatext=true
    -Dawt.useSystemAAFontSettings=lcd
    -Xss10M
    -Xmx1550M
    -ea
    -Xdebug
    -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n
    -XX:-HeapDumpOnOutOfMemoryError
    #{jrebel}
    -jar "#{Pathname.new(cyhome) + 'cytoscape.jar'}"
    -p "#{Pathname.new(cyhome) + 'plugins'}")
  cy_command.delete!("\n")
  system(cy_command)
end
task :cytoscape => [ :cytoscape_task ]

