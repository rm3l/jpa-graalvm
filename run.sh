#!/bin/sh

gu install ruby

java -Dgraal.ShowConfiguration=info -XX:+UseJVMCICompiler -XX:+EagerJVMCI -cp "target/jpa-graalvm-5.2.jar:target/lib/*" -Dprofile=datanucleus mydomain.MySampleApplication
