jpa-graalvm
===========

This project contains a set of Maven profiles that build and run the same application against other JPA providers.

- To build against [DataNucleus](http://datanucleus.org) (default profile): `mvn clean package` or `mvn clean package -P datanucleus`

- To build against [EclipseLink](http://www.eclipse.org/eclipselink/): `mvn clean package -P eclipselink`

- To build against [Hibernate](http://hibernate.org): `mvn clean package -P hibernate`

