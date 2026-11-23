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
import uk.gov.hmrc.sdecipaasstub.mappings.Identifier.*

enum StubId(val `type`: Identifier):

  case HDJ2123F extends StubId(`type` = ThreadReference)
  case HDJ2123G extends StubId(`type` = ThreadReference)
  case HDJ2123H extends StubId(`type` = ThreadReference)
  case WM111111D extends StubId(`type` = Nino)
  case ABC123 extends StubId(`type` = Pid)
  case DEF456 extends StubId(`type` = Pid)
  case GHI789 extends StubId(`type` = Pid)
  case JKL012 extends StubId(`type` = Pid)
  case Test extends StubId(`type` = TestOnly)
  case MissingFileTestOnly extends StubId(`type` = TestOnly)
  case MissingJsonTestOnly extends StubId(`type` = TestOnly)

enum Stub(val id: StubId, val description: String, val responsePath: String, val resultStatus: Status) {

  case MissingJsonTestOnlyStub
      extends Stub(
        StubId.MissingJsonTestOnly,
        "Stub to be used in StubMappingServiceSpec only",
        "responses/testOnlyInvalidContent.json",
        Ok
      )

  case TestOnlyStub
      extends Stub(StubId.Test, "Stub to be used in StubMappingServiceSpec only", "responses/testOnly.json", Ok)

  case MissingFileTestOnlyStub
      extends Stub(
        StubId.MissingFileTestOnly,
        "Stub to be used in StubMappingServiceSpec only",
        "responses/missing.json",
        Ok
      )

  case CreateThreadSuccess
      extends Stub(
        StubId.JKL012,
        "Stub that returns a thread creation success response",
        "responses/createThreadSuccess.json",
        Ok
      )
  case UpdateThreadSuccess
      extends Stub(
        StubId.HDJ2123G,
        "Stub that returns a thread update success response",
        "responses/updateThreadSuccess.json",
        Ok
      )
  case GetThreadSuccess
      extends Stub(StubId.HDJ2123F, "Stub that returns thread details", "responses/getThreadSuccess.json", Ok)
  case GetThreadSummarySuccess
      extends Stub(StubId.ABC123, "Stub that returns thread summary", "responses/getThreadSummarySuccess.json", Ok)
  case StaffUpdateSuccess
      extends Stub(StubId.DEF456, "Stub that returns staff update success", "responses/staffUpdateSuccess.json", Ok)
  case StaffInsertSuccess
      extends Stub(StubId.GHI789, "Stub that returns staff insert success", "responses/staffInsertSuccess.json", Ok)
  case FileSaveSuccess
      extends Stub(
        StubId.HDJ2123H,
        "Stub that returns file insert success response",
        "responses/saveFileSuccess.json",
        Ok
      )

}
