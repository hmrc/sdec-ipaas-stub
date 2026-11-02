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

package uk.gov.hmrc.sdecipaasstub.service.io

import cats.effect.IO
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.ThreadDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.service.algebra.{RecipientServiceAlgebra, StaffServiceAlgebra, TeamServiceAlgebra}
import uk.gov.hmrc.sdecipaasstub.service.impl.ThreadService

import javax.inject.{Inject, Singleton}

@Singleton
class IOThreadService @Inject() (
  threadDataAccess: ThreadDataAccessAlgebra[IO],
  teamService:      TeamServiceAlgebra[IO],
  staffService:     StaffServiceAlgebra[IO],
  recipientService: RecipientServiceAlgebra[IO]
) extends ThreadService[IO](threadDataAccess, teamService, staffService, recipientService) {}
