# Code assignment
A backend service written in Vert.x (https://vertx.io/) that keeps a list of services (defined by a URL), and periodically does a HTTP GET to each and saves the response ("OK" or "FAIL") to a database. Here we are using the Sqlite db.


# Completed Issues 

 - Added services persists across server restarts
 - Option to Delete individual services
 - Able to name services and remember when they were added
 - The HTTP poller implementation is completed
 - Protect the poller from misbehaving services (for example services responding really slowly)
 - Service URL's are validated before saving into DB

Frontend/Web track:
 - Option to Delete services 
  
  
# Pending Issue 

Frontend/Web track:
- We want full create/update/delete functionality for services
- The results from the poller are not automatically shown to the user (you have to reload the page to see results)
- We want to have informative and nice looking animations on add/remove services

Backend track
- Simultaneous writes sometimes causes strange behavior
- A user (with a different cookie/local storage) should not see the services added by another user

# Building

After clone the project import it as a gradle project in to one of your favourite IDEs. I have used IntelliJ for developing the project.
In intelliJ, choose the below steps for importing the project

```
New -> New from existing sources -> Import project from external model -> Gradle -> select "use gradle wrapper configuration"
```

You can run the project either from IntelliJ dirctly or using the below command from terminal
```
./gradlew clean run
```
