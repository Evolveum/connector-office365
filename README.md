#Microsoft Office 365 connector for midPoint (DEPRECATED)

This connector is **DEPRECATED**. Please consider use of [Graph API connector](https://github.com/Evolveum/connector-microsoft-graph-api) instead.

This connector was contributed to the midPoint project. However, due to lack of interest from midPoint subscribers the connector was not maintained. In the meantime Microsoft introduced cleaner and much more powerful method of user management for Microsoft cloud applications: Graph API. Therefore this connector was deprecated and there are no plans to support this connector any more. [Graph API connector](https://github.com/Evolveum/connector-microsoft-graph-api) is currently recommended method for interacting with Microsoft cloud applications.

#General Information:

Creates users within the Microsoft Azure Active Directory and assigns relevant 
licenses granting azure to Azure AD based services including Office 365.

Federated users now supported http://blogs.msdn.com/b/aadgraphteam/archive/2013/11/14/announcing-the-new-version-of-the-graph-api-api-version-2013-11-08.aspx

#Licenses:
Licenses are assigned as: <license name>:<plan component>:<plan component>  (note plan component option, omission means all plans)
When assigning a license the usageLocation needs to be set.


#LIMITATIONS
* Only currently handles users
* Does not currently support change of UPN

#Build:
* build:
    mvn package
* build with tests (requires proper connection parameters in config.groovy):
    mvn package -DskipTests=false


#TODO
* Group memberships
* Handle connectivity to o365 being unavailable - retry/cache events? / Test
* Multi valued attributes
* Allow UPN change 
* Test for query
