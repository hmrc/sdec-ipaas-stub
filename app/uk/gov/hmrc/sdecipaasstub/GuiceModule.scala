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

package uk.gov.hmrc.sdecipaasstub

import cats.effect.IO
import com.google.inject.{AbstractModule, TypeLiteral}
import doobie.util.transactor.Transactor
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.{StaffDataAccessAlgebra, StaffRoleDataAccessAlgebra, TeamDataAccessAlgebra}
import uk.gov.hmrc.sdecipaasstub.hcp.stubs.{IOStaffDataRepository, IOStaffRoleDataRepository, IOTeamDataRepository}
import uk.gov.hmrc.sdecipaasstub.infrastructure.DatabaseTransactorProvider
import uk.gov.hmrc.sdecipaasstub.service.algebra.StaffServiceAlgebra
import uk.gov.hmrc.sdecipaasstub.service.io.IOStaffService

class GuiceModule extends AbstractModule:
// bind(new TypeLiteral[] {}).to(classOf[])
  override def configure(): Unit =

    bind(new TypeLiteral[Transactor[IO]] {}).toProvider(classOf[DatabaseTransactorProvider])

    bind(new TypeLiteral[StaffDataAccessAlgebra[IO]] {}).to(classOf[IOStaffDataRepository])

    bind(new TypeLiteral[TeamDataAccessAlgebra[IO]] {}).to(classOf[IOTeamDataRepository])

    bind(new TypeLiteral[StaffRoleDataAccessAlgebra[IO]] {}).to(classOf[IOStaffRoleDataRepository])

    bind(new TypeLiteral[StaffServiceAlgebra[IO]] {}).to(classOf[IOStaffService])
