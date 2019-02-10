package mydomain;

import mydomain.model.AbstractBaseJpaEntity;
import mydomain.model.Address;
import mydomain.model.Person;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.graalvm.polyglot.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class MySampleApplication {

    private static EntityManagerFactory emf;

    public static void main(String[] args) {

        emf = Persistence.createEntityManagerFactory(
                "mytest_" + System.getProperty("profile"));

        executeInTransaction((em) -> {
            final Set<Person> resultSet =
                    new HashSet<>(em.createQuery("SELECT p FROM Person p", Person.class).getResultList());
            assertTrue(resultSet.isEmpty());
        });

        final Set<Person> personList = IntStream.range(1, 3)
                .mapToObj(i -> {
                    final Person person = new Person.Builder().uniqueName("person " + i).build();
                    person.setLastAddress(new Address.Builder()
                            .person(person)
                            .number(Integer.toUnsignedLong(i))
                            .street("Avenue FHB")
                            .zipCode(Integer.toUnsignedLong(i + 20))
                            .countryCode(i % 2 == 0 ? "CI" : "FR")
                            .city(i %2 == 0 ? "Abidjan" : "Lyon")
                            .build());
                    return person;
                })
                .collect(Collectors.toSet());

        //Persist with version
        executeInTransaction((em) -> personList.forEach(em::persist));

        //Perform an operation
        final Map<Long, Person> newPersonList = personList.stream()
                .collect(Collectors.toMap(AbstractBaseJpaEntity::getId,
                        p -> new Person.Builder()
                                .uuid(p.getUuid())
                                .uniqueName(p.getUniqueName() + " (renamed)").build()));

        //Now read them all
        executeInTransaction((em) -> {
            final Set<Person> resultSet =
                    new HashSet<>(em.createQuery("SELECT p FROM Person p", Person.class).getResultList());

            assertEquals(personList, resultSet);

            resultSet.forEach(p -> {
                p.setUniqueName(newPersonList.get(p.getId()).getUniqueName());
            });
        });

        executeInTransaction((em) -> {
            final Set<Person> resultSet =
                    new HashSet<>(em.createQuery("SELECT p FROM Person p", Person.class).getResultList());

            assertEquals(new HashSet<>(newPersonList.values()), resultSet);
            resultSet.forEach(p -> assertEquals(newPersonList.get(p.getId()).getUniqueName(), p.getUniqueName()));

            prettyPrintWithRuby(resultSet);

        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (emf.isOpen()) {
                emf.close();
            }
        }));

    }

    private static void prettyPrintWithJS(final Collection<Person> people) {
        try (final Context context = Context.create("js")) {
            final Value parse = context.eval("js", "JSON.parse");
            final Value stringify = context.eval("js", "JSON.stringify");
            final Value parseResult = parse.execute("[" + people.stream()
                    .map(Person::toJsonString)
                    .collect(Collectors.joining(", ")) + "]");
            System.out.println(stringify.execute(parseResult, null, 2).asString());
        }
    }

    private static void prettyPrintWithRuby(final Collection<Person> people) {
        try (final Context context = Context.create("ruby")) {
            final Value json = context.eval("ruby",
                    "require 'json'; " +
                    "JSON.pretty_generate(" +
                    " JSON.parse('" +
                    "   [" + people.stream()
                    .map(Person::toJsonString)
                    .collect(Collectors.joining(", ")) + "]'))");
            System.out.println(json.asString());
        }
    }

    private static void executeInTransaction(final TestRunnable runnable) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try
        {
            tx.begin();

            runnable.run(em);

            em.flush();

            tx.commit();
        }
        catch (Throwable thr)
        {
            throw new IllegalStateException("Failed test : " + thr.getMessage());
        }
        finally
        {
            if (tx.isActive())
            {
                tx.rollback();
            }
            em.close();
        }
    }

    @FunctionalInterface
    private interface TestRunnable {
        void run(EntityManager em);
    }
}
