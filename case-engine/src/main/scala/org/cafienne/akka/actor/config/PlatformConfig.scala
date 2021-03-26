package org.cafienne.akka.actor.config

import org.cafienne.akka.actor.config.util.MandatoryConfig
import org.cafienne.akka.actor.identity.TenantUser

import java.util.List

class PlatformConfig(val parent: CafienneConfig) extends MandatoryConfig {
  override def path = "platform"

  val platformOwners: List[String] = config.getStringList("owners")
  if (platformOwners.isEmpty) {
    fail("Platform owners cannot be an empty list. Check configuration property cafienne.platform.owners")
  }

  lazy val defaultTenant = {
    val configuredDefaultTenant = readString("default-tenant")
    configuredDefaultTenant
  }

  /**
    * Config property for reading a specific file with bootstrap tenant setup
    */
  lazy val bootstrapFile = readString("bootstrap-file")

  def isPlatformOwner(user: TenantUser): Boolean = isPlatformOwner(user.id)

  def isPlatformOwner(userId: String): Boolean = {
    // TTP: platformOwners should be taken as Set and "toLowerCase" initially, and then we can do "contains" instead
    logger.debug("Checking whether user " + userId + " is a platform owner; list of owners: " + platformOwners)
    platformOwners.stream().filter(o => o.equalsIgnoreCase(userId)).count() > 0
  }
}