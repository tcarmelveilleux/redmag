# Redmine AuthZ Generator (redmag) #

## What is this tool for ? ##
This tool can be used to centralize the management of multiple SVN repositories from a single Redmine installation (tested on 0.8.2 and 0.8.3).<p>

The tool must be run through a cron job or as a project creation hook. It assumes that your SVN is hosted through Apache2 with AuthZ access-control and authentication on the Redmine database through mod_auth_mysql (information provided on this project on how to do this).<br>
<br>
<br>
<h2>What does it do ?</h2>
<ul><li>Connects to the Redmine database to gather all necessary project, user, members and role information<br>
</li><li>Generates missing SVN repositories based on the projects.<br>
</li><li>Generates a single master AuthZ access control configuration to only allow members of each project to access the related repositories and paths.</li></ul>

<h2>Why was it created ?</h2>
This tool was designed to allow the creation of an integrated multi-project collaboration platform with strong protection against unauthorized access between repositories.<br>
<br>
This is particularly useful in academic environments where IT resources are limited but where sensitive proprietary information might be found on a project-by-project basis. With this tool, access control can be managed centrally.