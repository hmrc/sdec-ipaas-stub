
# sdec-ipaas-stub
This is a Boomi emulator for testing.

## Authentication and Authorisation
TODO 8-(

## REST API

### Health Check
The following gets the system up and running, useful for testing etc.
```http request
GET http://localhost:4999/ping/ping
```

### Staff and Team
The following endpoint is a call that will be normally made when the HMRC staff lands on the workspace page:


The following will insert a single SRS enrollment, if it does not already exist:
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

## Creating a new stub
Add an `StubId` and `Stub`to `Stub.scala` the stubId should be some unique field or value from the incoming request
```
case JNM4827Q extends StubId(`type` = ThreadReference)
case MyStub extends Stub(StubId.JNM4827Q, "some description of the stub", "responses/someResponse.json", Ok)
```
add your response file to `/resource/responses` in the above example someResponse.json

## Further Reading
- [Rock the JVM Tagless-Final](https://rockthejvm.com/articles/tagless-final-in-scala)
- [Baeldung Tagless-Final](https://www.baeldung.com/scala/tagless-final-pattern)
- [Scala Cats](https://typelevel.org/cats/)
- [Cats-Effect](https://typelevel.org/cats-effect/docs/getting-started)

### License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
