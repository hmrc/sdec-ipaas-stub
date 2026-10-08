
# sdec-ipaas-stub
This is a Boomi emulator for testing.

## Authentication and Authorisation
TODO 8-(

## REST API

### Staff and Team
The following will insert, if it does not already exist:
```http request
POST http://localhost:4999/sdec-ipaas-stub/api/v1/staff
Content-Type: application/json

{
"pid": "1010",
"name": "John Tester",
"email": "j.t@test.com",
"srs": "SDEC_VAT_User"
}
```

## MFT (File Upload Service)

### License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

## Database
You can connect to the built-in H2 database using SQL IDE, such as DataGrip. Use the following:
```shell
user = sa
password =
URL = jdbc:h2:tcp://localhost:9092/mem:sdec
```

## Code Structure
1. Implement your algebra as a trait in F - see `StaffServiceAlgebra`
2. Implement the algebra, keeping it using the `tagless-final` design pattern - see `StaffService`
3. Implement an IO version - see `IOStaffService`
4. Bind it in `GuiceModule`
5. Add `IOActionBuilder` in the controller class parameters - see `StaffController`

### `AplicationLogger`
Classes can extend `ApplicationLogger` that will make available `logger` in F[_] that can be used your implementations. See
`StaffController` or `StaffService` for examples.

## Further Reading
- [Rock the JVM Tagless-Final](https://rockthejvm.com/articles/tagless-final-in-scala)
- [Baeldung Tagless-Final](https://www.baeldung.com/scala/tagless-final-pattern)
- [Scala Cats](https://typelevel.org/cats/)
- [Cats-Effect](https://typelevel.org/cats-effect/docs/getting-started)
- [Doobie](https://typelevel.org/doobie/)