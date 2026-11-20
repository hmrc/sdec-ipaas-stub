/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.sdecipaasstub.mappings

import play.api.mvc.Results.{Ok, Status}

enum StubId(val description: String):
  case TESTONLY extends StubId(description = "testOnly Identifier")
  case HDJ2123F extends StubId(description = "thread Id for get thread")

enum Stub(val id: StubId, val description: String, val responsePath: String, val resultStatus: Status) {
  case GetThreadSuccess
      extends Stub(
        StubId.HDJ2123F,
        "Stub that returns a successful IpaaS response",
        "responses/getThreadSuccess.json",
        Ok
      )
  case TestOnlyStub
      extends Stub(StubId.TESTONLY, "Stub to be used in StubMappingServiceSpec only", "responses/testOnly.json", Ok)
}
