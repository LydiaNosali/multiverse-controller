package io.nms.central.microservice.topology.impl;

public class ApiSql {

	/*-------------------- TABLE CREATION --------------------*/
	public static final String CREATE_TABLE_VSUBNET = "CREATE TABLE IF NOT EXISTS `Vsubnet` (\n" +
			"    `id` INT NOT NULL AUTO_INCREMENT,\n" +
			"    `name` VARCHAR(127) NOT NULL UNIQUE,\n" + 
			"    `label` VARCHAR(255) NOT NULL,\n" + 
			"    `description` VARCHAR(255) NOT NULL,\n" +
			"    `type` VARCHAR(50) NOT NULL,\n" +
			"	 `info` JSON DEFAULT NULL,\n" +
			"    `created` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" + 
			"    `updated` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" + 
			"    PRIMARY KEY (`id`)\n" +
			")";
	public static final String CREATE_TABLE_VNODE = "CREATE TABLE IF NOT EXISTS `Vnode` (\n" +
			"    `id` INT NOT NULL AUTO_INCREMENT,\n" +
			"    `name` VARCHAR(127) NOT NULL,\n" + 
			"    `label` VARCHAR(255) NOT NULL,\n" + 
			"    `description` VARCHAR(255) NOT NULL,\n" +
			"	 `info` JSON DEFAULT NULL,\n" +
			"    `status` VARCHAR(10) NOT NULL,\n" +
			"    `created` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" + 
			"    `updated` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" + 
			"    `posx` INT,\n" + 
			"    `posy` INT,\n" + 
			"    `location` VARCHAR(50),\n" + 
			"    `type` VARCHAR(50) NOT NULL,\n" +
			"    `vsubnetId` INT NOT NULL,\n" +
			"    `hwaddr` VARCHAR(50) NOT NULL UNIQUE,\n" + 
			"    `mgmtIp` VARCHAR(20) NOT NULL UNIQUE,\n" + 
			"    PRIMARY KEY (`id`),\n" +
			"    UNIQUE KEY (`name`, `vsubnetId`),\n" +
			"    FOREIGN KEY (`vsubnetId`)\n" + 
			"    	REFERENCES Vsubnet(`id`)\n" + 
			"       ON DELETE CASCADE\n" + 
			"		ON UPDATE CASCADE\n" + 
			")";
	public static final String CREATE_TABLE_VLTP = "CREATE TABLE IF NOT EXISTS Vltp (\n" +
			"    `id` INT NOT NULL AUTO_INCREMENT,\n" +
			"    `name` VARCHAR(127) NOT NULL,\n" + 
			"    `label` VARCHAR(127) NOT NULL,\n" + 
			"    `description` VARCHAR(255) NOT NULL,\n" +
			"	 `info` JSON DEFAULT NULL,\n" +
			"    `status` VARCHAR(10) NOT NULL,\n" +
			"    `created` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" + 
			"    `updated` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
			"    `vnodeId` INT NOT NULL,\n" + 
			"    `port` VARCHAR(255) NOT NULL,\n" +
			"    `bandwidth` VARCHAR(100) NOT NULL,\n" +
			"    `mtu` INT NOT NULL,\n" +
			"    `busy` BOOLEAN NOT NULL,\n"+
			"    PRIMARY KEY (`id`),\n" +
			"    UNIQUE KEY (`name`, `vnodeId`),\n" +
			"    FOREIGN KEY (`vnodeId`) \n" + 
			"        REFERENCES Vnode(`id`)\n" + 
			"        ON DELETE CASCADE\n" + 
			"		 ON UPDATE CASCADE\n" + 
			")";
	public static final String CREATE_TABLE_VCTP = "CREATE TABLE IF NOT EXISTS Vctp (\n" +
			"    `id` INT NOT NULL AUTO_INCREMENT,\n" +
			"    `name` VARCHAR(127) NOT NULL,\n" + 
			"    `label` VARCHAR(127) NOT NULL,\n" + 
			"    `description` VARCHAR(255),\n" +
			"	 `info` JSON DEFAULT NULL,\n" +
			"    `connType` VARCHAR(10) NOT NULL,\n" +
			"	 `connInfo` JSON NOT NULL,\n" +
			"    `status` VARCHAR(10) NOT NULL,\n" +
			"    `created` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" + 
			"    `updated` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
			"    `vltpId` INT NULL,\n" +
			"    `vctpId` INT NULL,\n" +
			"    `vnodeId` INT NOT NULL,\n" +
			"    PRIMARY KEY (`id`),\n" + 
			"    UNIQUE KEY (`name`, `vnodeId`),\n" +
			"    FOREIGN KEY (`vltpId`) \n" + 
			"       REFERENCES Vltp(`id`)\n" + 
			"       ON DELETE CASCADE\n" + 
			"		ON UPDATE CASCADE,\n" +
			"    FOREIGN KEY (`vctpId`) \n" + 
			"       REFERENCES Vctp(`id`)\n" + 
			"       ON DELETE CASCADE\n" + 
			"		ON UPDATE CASCADE,\n" +
			"    FOREIGN KEY (`vnodeId`) \n" + 
			"       REFERENCES Vnode(`id`)\n" + 
			"       ON DELETE CASCADE\n" + 
			"		ON UPDATE CASCADE\n" +
			")";
	public static final String CREATE_TABLE_VLINK = "CREATE TABLE IF NOT EXISTS Vlink (\n" +
			"    `id` INT NOT NULL AUTO_INCREMENT,\n" +
			"    `name` VARCHAR(127) NOT NULL UNIQUE,\n" + 
			"    `label` VARCHAR(127) NOT NULL,\n" + 
			"    `description` VARCHAR(255),\n" +
			"	 `info` JSON DEFAULT NULL,\n" +
			"    `status` VARCHAR(10) NOT NULL,\n" +
			"    `created` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" + 
			"    `updated` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
			"    `srcVltpId` INT NOT NULL UNIQUE,\n" + 
			"    `destVltpId` INT NOT NULL UNIQUE,\n" + 
			"    PRIMARY KEY (`id`),\n" + 
			"    FOREIGN KEY (`srcVltpId`) \n" + 
			"        REFERENCES Vltp(`id`)\n" + 
			"        ON DELETE CASCADE\n" + 
			"        ON UPDATE CASCADE,\n" + 
			"    FOREIGN KEY (`destVltpId`) \n" + 
			"        REFERENCES Vltp(`id`)\n" + 
			"        ON DELETE CASCADE\n" + 
			"        ON UPDATE CASCADE\n" +
			")";
	public static final String CREATE_TABLE_VLINKCONN = "CREATE TABLE IF NOT EXISTS VlinkConn (\n" +
			"    `id` INT NOT NULL AUTO_INCREMENT,\n" +
			"    `name` VARCHAR(127) NOT NULL UNIQUE,\n" + 
			"    `label` VARCHAR(127) NOT NULL,\n" + 
			"    `description` VARCHAR(255) NOT NULL,\n" +
			"	 `info` JSON DEFAULT NULL,\n" +
			"    `status` VARCHAR(10) NOT NULL,\n" +
			"    `created` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" + 
			"    `updated` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
			"    `vlinkId` INT NOT NULL,\n" +
			"    `srcVctpId` INT NOT NULL UNIQUE,\n" + 
			"    `destVctpId` INT NOT NULL UNIQUE,\n" +
			"    PRIMARY KEY (`id`),\n" + 
			"    FOREIGN KEY (`vlinkId`) \n" + 
			"        REFERENCES Vlink(`id`)\n" + 
			"        ON DELETE CASCADE\n" + 
			"        ON UPDATE CASCADE,\n" +
			"    FOREIGN KEY (`srcVctpId`) \n" + 
			"        REFERENCES Vctp(`id`)\n" + 
			"        ON DELETE CASCADE\n" + 
			"        ON UPDATE CASCADE,\n" + 
			"    FOREIGN KEY (`destVctpId`) \n" + 
			"        REFERENCES Vctp(`id`)\n" + 
			"        ON DELETE CASCADE\n" + 
			"        ON UPDATE CASCADE\n" + 
			")";
	public static final String CREATE_TABLE_VCONNECTION = "CREATE TABLE IF NOT EXISTS Vconnection (\n" +
			"    `id` INT NOT NULL AUTO_INCREMENT,\n" +
			"    `name` VARCHAR(127) NOT NULL UNIQUE,\n" + 
			"    `label` VARCHAR(127) NOT NULL,\n" + 
			"    `description` VARCHAR(255) NOT NULL,\n" +
			"	 `info` JSON DEFAULT NULL,\n" +
			"    `status` VARCHAR(10) NOT NULL,\n" +
			"    `created` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" + 
			"    `updated` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
			"    `srcVctpId` INT NOT NULL UNIQUE,\n" + 
			"    `destVctpId` INT NOT NULL UNIQUE,\n" +
			"    PRIMARY KEY (`id`),\n" +
			"    FOREIGN KEY (`srcVctpId`) \n" + 
			"        REFERENCES Vctp(`id`)\n" + 
			"        ON DELETE CASCADE\n" + 
			"        ON UPDATE CASCADE,\n" + 
			"    FOREIGN KEY (`destVctpId`) \n" + 
			"        REFERENCES Vctp(`id`)\n" + 
			"        ON DELETE CASCADE\n" + 
			"        ON UPDATE CASCADE\n" + 
			")";
	public static final String CREATE_TABLE_VTRAIL = "CREATE TABLE IF NOT EXISTS Vtrail (\n" +
			"    `id` INT NOT NULL AUTO_INCREMENT,\n" +
			"    `name` VARCHAR(127) NOT NULL UNIQUE,\n" + 
			"    `label` VARCHAR(127) NOT NULL,\n" + 
			"    `description` VARCHAR(255),\n" +
			"	 `info` JSON DEFAULT NULL,\n" +
			"    `status` VARCHAR(10) NOT NULL,\n" +
			"    `created` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" + 
			"    `updated` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
			"    `srcVnodeId` INT NOT NULL UNIQUE,\n" + 
			"    `trgtVnodeId` INT NOT NULL UNIQUE,\n" + 
			"    PRIMARY KEY (`id`),\n" + 
			"    FOREIGN KEY (`srcVnodeId`) \n" + 
			"        REFERENCES Vnode(`id`)\n" + 
			"        ON DELETE CASCADE\n" + 
			"        ON UPDATE CASCADE,\n" + 
			"    FOREIGN KEY (`trgtVnodeId`) \n" + 
			"        REFERENCES Vnode(`id`)\n" + 
			"        ON DELETE CASCADE\n" + 
			"        ON UPDATE CASCADE\n" +
			")";
	public static final String CREATE_TABLE_VCROSS_CONNECT = "CREATE TABLE IF NOT EXISTS `VcrossConnect` (\n" +
			"    `id` INT NOT NULL AUTO_INCREMENT,\n" +
			"    `name` VARCHAR(127) NOT NULL,\n" + 
			"    `label` VARCHAR(255) NOT NULL,\n" + 
			"    `description` VARCHAR(255) NOT NULL,\n" +
			"	 `info` JSON DEFAULT NULL,\n" +
			"    `status` VARCHAR(10) NOT NULL,\n" +
			"    `trailId` INT NOT NULL,\n" +
			"    `switchId` INT NOT NULL,\n" +
			"    `ingressPortId` INT NOT NULL UNIQUE,\n" +
			"    `egressPortId` INT NOT NULL UNIQUE,\n" +
			"    `created` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" + 
			"    `updated` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
			"    UNIQUE KEY (`ingressPortId`, `egressPortId`),\n" +
			"    PRIMARY KEY (`id`),\n" + 
			"    FOREIGN KEY (`ingressPortId`) \n" + 
			"       REFERENCES Vltp(`id`)\n" + 
			"       ON DELETE CASCADE\n" + 
			"		ON UPDATE CASCADE,\n" +
			"    FOREIGN KEY (`egressPortId`) \n" +
			"       REFERENCES Vltp(`id`)\n" + 
			"       ON DELETE CASCADE\n" + 
			"		ON UPDATE CASCADE,\n" +
			"    FOREIGN KEY (`switchId`) \n" + 
			"       REFERENCES Vnode(`id`)\n" + 
			"       ON DELETE CASCADE\n" + 
			"		ON UPDATE CASCADE,\n" +
			"    FOREIGN KEY (`trailId`) \n" + 
			"       REFERENCES Vtrail(`id`)\n" + 
			"       ON DELETE CASCADE\n" + 
			"		ON UPDATE CASCADE\n" +
			")";
	public static final String CREATE_TABLE_PREFIX = "CREATE TABLE IF NOT EXISTS Prefix (\n" +
			"    `id` INT NOT NULL AUTO_INCREMENT,\n" +
			"    `name` VARCHAR(255) NOT NULL,\n" + 
			"    `originId` INT NOT NULL,\n" +
			"    `available` BOOLEAN NOT NULL,\n" +
			"    `created` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" + 
			"    `updated` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
			"    PRIMARY KEY (`id`),\n" +
			"    UNIQUE KEY (`name`, `originId`),\n" +
			"    FOREIGN KEY (`originId`) \n" + 
			"        REFERENCES Vnode(`id`)\n" + 
			"        ON DELETE CASCADE\n" + 
			"		 ON UPDATE CASCADE\n" +
			")";

	/*-------------------- INSERT ITEMS --------------------*/	
	public static final String INSERT_VSUBNET = "INSERT INTO Vsubnet (name, label, description, type, info) VALUES (?, ?, ?, ?, ?) ";
	public static final String INSERT_VNODE = "INSERT INTO Vnode (name, label, description, info, status, posx, posy, location, type, vsubnetId, hwaddr, mgmtIp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
	public static final String INSERT_VLTP = "INSERT INTO Vltp (name, label, description, info, status, vnodeId, port, bandwidth, mtu, busy) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
	public static final String INSERT_VCTP_VCTP = "INSERT INTO Vctp (name, label, description, info, connType, connInfo, status, vctpId, vnodeId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ";
	public static final String INSERT_VCTP_VLTP = "INSERT INTO Vctp (name, label, description, info, connType, connInfo, status, vltpId, vnodeId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ";
	public static final String INSERT_VLINK = "INSERT INTO Vlink (name, label, description, info, status, srcVltpId, destVltpId) VALUES (?, ?, ?, ?, ?, ?, ?) ";
	public static final String INSERT_VLINKCONN = "INSERT INTO VlinkConn (name, label, description, info, status, srcVctpId, destVctpId, vlinkId) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ";
	public static final String INSERT_VCONNECTION = "INSERT INTO Vconnection (name, label, description, info, status, srcVctpId, destVctpId) VALUES (?, ?, ?, ?, ?, ?, ?) ";
	public static final String INSERT_VTRAIL = "INSERT INTO Vtrail (name, label, description, info, status, srcVnodeId, trgtVnodeId) VALUES (?, ?, ?, ?, ?, ?, ?) ";
	public static final String INSERT_VCROSS_CONNECT = "INSERT INTO VcrossConnect (name, label, description, info, status, trailId, switchId, ingressPortId, egressPortId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ";

	// insert ignore for PUT
	public static final String INSERT_PREFIX = "INSERT IGNORE INTO Prefix (name, originId, available) VALUES (?, ?, ?)";
	
	/*-------------------- FETCH ALL ITEMS --------------------*/
	public static final String FETCH_ALL_VSUBNETS = "SELECT "
			+ "id, name, label, description, type, info, created, updated FROM Vsubnet";
	// public static final String FETCH_ALL_VNODES = "SELECT "
	//		+ "id, name, label, description, info, status, created, updated, posx, posy, location, type, vsubnetId, hwaddr, mgmtIp FROM Vnode";
	// public static final String FETCH_ALL_VLTPS = "SELECT "
	//		+ "id, name, label, description, info, status, created, updated, vnodeId, port, bandwidth, mtu, busy FROM Vltp";
	// public static final String FETCH_ALL_VCTPS = "SELECT "
	//		+ "id, name, label, description, info, created, updated, connType, connInfo, status, vltpId, vctpId, vnodeId FROM Vctp"; 
	// public static final String FETCH_ALL_VLINKS = "SELECT "
	//		+ "Vlink.id, Vlink.name, Vlink.label, Vlink.description, Vlink.info, Vlink.status, Vlink.created, Vlink.updated, "
	//		+ "Vlink.srcVltpId, Vlink.destVltpId, "
	//		+ "s.vnodeId AS srcVnodeId, d.vnodeId AS destVnodeId "
	//		+ "FROM ((Vlink INNER JOIN Vltp AS s ON Vlink.srcVltpId=s.id) INNER JOIN Vltp AS d ON Vlink.destVltpId=d.id)";
	// public static final String FETCH_ALL_VLINKCONNS = "SELECT "
	//		+ "VlinkConn.id, VlinkConn.name, VlinkConn.label, VlinkConn.description, VlinkConn.info, VlinkConn.status, "
	//		+ "VlinkConn.created, VlinkConn.updated, VlinkConn.srcVctpId, VlinkConn.destVctpId, VlinkConn.vlinkId, "
	//		+ "sCtp.vltpId AS srcVltpId, dCtp.vltpId AS destVltpId, sLtp.vnodeId AS srcVnodeId, dLtp.vnodeId AS destVnodeId "
	//		+ "FROM ((VlinkConn "
	//		+ "INNER JOIN Vctp AS sCtp ON VlinkConn.srcVctpId=sCtp.id INNER JOIN Vltp AS sLtp ON sCtp.vltpId=sLtp.id) "
	//		+ "INNER JOIN Vctp AS dCtp ON VlinkConn.destVctpId=dCtp.id INNER JOIN Vltp AS dLtp ON dCtp.vltpId=dLtp.id)";
	// public static final String FETCH_ALL_VCONNECTIONS = "SELECT "
	//		+ "Vconnection.id, Vconnection.name, Vconnection.label, Vconnection.description, Vconnection.info, Vconnection.status, "
	//		+ "Vconnection.created, Vconnection.updated, Vconnection.srcVctpId, Vconnection.destVctpId, "
	//		+ "sCtp.vnodeId AS srcVnodeId, dCtp.vnodeId AS destVnodeId "
	//		+ "FROM ((Vconnection "
	//		+ "INNER JOIN Vctp AS sCtp ON Vconnection.srcVctpId=sCtp.id) "
	//		+ "INNER JOIN Vctp AS dCtp ON Vconnection.destVctpId=dCtp.id)";
	public static final String FETCH_ALL_PREFIXES = "SELECT "
			+ "id, name, originId, available, created, updated FROM Prefix";

	public static final String FETCH_VSUBNETS_BY_TYPE = "SELECT "
			+ "id, name, label, description, type, info, created, updated "
			+ "FROM Vsubnet WHERE type = ?";
	public static final String FETCH_VNODES_BY_VSUBNET = "SELECT "
			+ "id, name, label, description, info, status, created, updated, posx, posy, location, type, vsubnetId, hwaddr, mgmtIp "
			+ "FROM Vnode WHERE vsubnetId = ?";
	public static final String FETCH_VNODES_BY_TYPE = "SELECT "
			+ "id, name, label, description, info, status, created, updated, posx, posy, location, type, vsubnetId, hwaddr, mgmtIp "
			+ "FROM Vnode WHERE type = ?";
	public static final String FETCH_VLINKS_BY_VSUBNET = "SELECT "
			+ "Vlink.id, Vlink.name, Vlink.label, Vlink.description, Vlink.info, Vlink.status, Vlink.created, Vlink.updated, "
			+ "Vlink.srcVltpId, Vlink.destVltpId, " 
			+ "Vltp.vnodeId AS srcVnodeId, destLtp.vnodeId AS destVnodeId " 
			+ "FROM Vnode "
			+ "INNER JOIN Vltp ON Vnode.id=Vltp.vnodeId "
			+ "INNER JOIN Vlink ON Vltp.id=Vlink.srcVltpId INNER JOIN Vltp as destLtp ON Vlink.destVltpId=destLtp.id "
			+ "WHERE Vnode.vsubnetId = ?";
	public static final String FETCH_VLINKCONNS_BY_VSUBNET = "SELECT "
			+ "VlinkConn.id, VlinkConn.name, VlinkConn.label, VlinkConn.description, VlinkConn.info, VlinkConn.status, VlinkConn.created, VlinkConn.updated, "
			+ "VlinkConn.srcVctpId, VlinkConn.destVctpId, VlinkConn.vlinkId, "
			+ "Vltp.id AS srcVltpId, destLtp.id AS destVltpId, "
			+ "Vltp.vnodeId AS srcVnodeId, destLtp.vnodeId AS destVnodeId "
			+ "FROM Vnode "
			+ "INNER JOIN Vltp ON Vnode.id=Vltp.vnodeId INNER JOIN Vctp ON Vltp.id=Vctp.vltpId "
			+ "INNER JOIN VlinkConn ON Vctp.id=VlinkConn.srcVctpId INNER JOIN Vctp as destCtp ON VlinkConn.destVctpId=destCtp.id "
			+ "INNER JOIN Vltp AS destLtp ON destCtp.vltpId=destLtp.id "
			+ "WHERE Vnode.vsubnetId = ?";
	public static final String FETCH_VCONNECTIONS_BY_TYPE = "SELECT "
			+ "Vconnection.id, Vconnection.name, Vconnection.label, Vconnection.description, Vconnection.info, Vconnection.status, "
			+ "Vconnection.created, Vconnection.updated, Vconnection.srcVctpId, Vconnection.destVctpId, "
			+ "sCtp.vnodeId AS srcVnodeId, dCtp.vnodeId AS destVnodeId "
			+ "FROM ((Vconnection "
			+ "INNER JOIN Vctp AS sCtp ON Vconnection.srcVctpId=sCtp.id) "
			+ "INNER JOIN Vctp AS dCtp ON Vconnection.destVctpId=dCtp.id) "
			+ "WHERE sCtp.connType = ?";
	public static final String FETCH_VCONNECTIONS_BY_VSUBNET_BY_TYPE = "SELECT "
			+ "Vconnection.id, Vconnection.name, Vconnection.label, Vconnection.description, Vconnection.info, Vconnection.status, Vconnection.created, Vconnection.updated, "
			+ "Vconnection.srcVctpId, Vconnection.destVctpId, "
			+ "sCtp.vnodeId AS srcVnodeId, dCtp.vnodeId AS destVnodeId "
			+ "FROM Vnode "
			+ "INNER JOIN Vctp as sCtp ON Vnode.id=sCtp.vnodeId "
			+ "INNER JOIN Vconnection ON sCtp.id=Vconnection.srcVctpId "
			+ "INNER JOIN Vctp as dCtp ON Vconnection.destVctpId=dCtp.id "
			+ "WHERE Vnode.vsubnetId = ? AND sCtp.connType = ?";
	public static final String FETCH_VCONNECTIONS_BY_VSUBNET = "SELECT "
			+ "Vconnection.id, Vconnection.name, Vconnection.label, Vconnection.description, Vconnection.info, Vconnection.status, Vconnection.created, Vconnection.updated, "
			+ "Vconnection.srcVctpId, Vconnection.destVctpId, "
			+ "sCtp.vnodeId AS srcVnodeId, dCtp.vnodeId AS destVnodeId "
			+ "FROM Vnode "
			+ "INNER JOIN Vctp as sCtp ON Vnode.id=sCtp.vnodeId "
			+ "INNER JOIN Vconnection ON sCtp.id=Vconnection.srcVctpId "
			+ "INNER JOIN Vctp as dCtp ON Vconnection.destVctpId=dCtp.id "
			+ "WHERE Vnode.vsubnetId = ?";
	public static final String FETCH_PREFIXES_BY_VSUBNET = "SELECT "
			+ "Prefix.id, Prefix.name, Prefix.originId, Prefix.available, Prefix.created, Prefix.updated "
			+ "FROM Vnode "
			+ "INNER JOIN Prefix ON Vnode.id=Prefix.originId "
			+ "WHERE Vnode.vsubnetId = ?";
	public static final String FETCH_VLTPS_BY_VNODE = "SELECT "
			+ "id, name, label, description, info, status, created, updated, vnodeId, port, bandwidth, mtu, busy "
			+ "FROM Vltp WHERE vnodeId = ?";
	public static final String FETCH_VCTPS_BY_TYPE = "SELECT "
			+ "id, name, label, description, info, created, updated, connType, connInfo, status, vltpId, vctpId, vnodeId FROM Vctp WHERE connType = ?";
	public static final String FETCH_VCTPS_BY_VLTP = "SELECT "
			+ "id, name, label, description, info, created, updated, connType, connInfo, status, vltpId, vnodeId FROM Vctp WHERE vltpId = ?";
	public static final String FETCH_VCTPS_BY_VCTP = "SELECT "
			+ "id, name, label, description, info, created, updated, connType, connInfo, status, vctpId, vnodeId FROM Vctp WHERE vctpId = ?";
	public static final String FETCH_VCTPS_BY_VNODE = "SELECT "
			+ "id, name, label, description, info, created, updated, connType, connInfo, status, vltpId, vctpId, vnodeId FROM Vctp WHERE vnodeId = ?";
	public static final String FETCH_VLINKCONNS_BY_VLINK = "SELECT "
			+ "VlinkConn.id, VlinkConn.name, VlinkConn.label, VlinkConn.description, VlinkConn.info, VlinkConn.status, "
			+ "VlinkConn.created, VlinkConn.updated, VlinkConn.srcVctpId, VlinkConn.destVctpId, VlinkConn.vlinkId, "
			+ "sCtp.vltpId AS srcVltpId, dCtp.vltpId AS destVltpId, sLtp.vnodeId AS srcVnodeId, dLtp.vnodeId AS destVnodeId "
			+ "FROM ((VlinkConn "
			+ "INNER JOIN Vctp AS sCtp ON VlinkConn.srcVctpId=sCtp.id INNER JOIN Vltp AS sLtp ON sCtp.vltpId=sLtp.id) "
			+ "INNER JOIN Vctp AS dCtp ON VlinkConn.destVctpId=dCtp.id INNER JOIN Vltp AS dLtp ON dCtp.vltpId=dLtp.id) "
			+ "WHERE VlinkConn.vlinkId = ?";
	public static final String FETCH_VTRAILS_BY_VSUBNET = "SELECT "
			+ "Vtrail.id, Vtrail.name, Vtrail.label, Vtrail.description, Vtrail.info, Vtrail.status, Vtrail.created, Vtrail.updated, "
			+ "Vtrail.srcVnodeId, Vtrail.trgtVnodeId " 
			+ "FROM Vtrail "
			+ "INNER JOIN Vnode ON Vnode.id=Vtrail.srcVnodeId "
			+ "WHERE Vnode.vsubnetId = ?";
	public static final String FETCH_VCROSS_CONNECTS_BY_NODE = "SELECT "
			+ "VcrossConnect.id, VcrossConnect.name, VcrossConnect.label, VcrossConnect.description, "
			+ "VcrossConnect.info, VcrossConnect.status, VcrossConnect.trailId, "
			+ "VcrossConnect.switchId, Vnode.mgmtIp as switchIpAddr, VcrossConnect.ingressPortId, VcrossConnect.egressPortId, "
			+ "VcrossConnect.created, VcrossConnect.updated "
			+ "FROM VcrossConnect INNER JOIN Vnode ON VcrossConnect.switchId = Vnode.id WHERE Vnode.id = ?";
	public static final String FETCH_VCROSS_CONNECTS_BY_TRAIL = "SELECT "
			+ "VcrossConnect.id, VcrossConnect.name, VcrossConnect.label, VcrossConnect.description, "
			+ "VcrossConnect.info, VcrossConnect.status, VcrossConnect.trailId, "
			+ "VcrossConnect.switchId, Vnode.mgmtIp as switchIpAddr, VcrossConnect.ingressPortId, VcrossConnect.egressPortId, "
			+ "VcrossConnect.created, VcrossConnect.updated "
			+ "FROM VcrossConnect INNER JOIN Vnode ON VcrossConnect.switchId = Vnode.id WHERE VcrossConnect.trailId = ?";
	public static final String FETCH_PREFIXES_BY_NODE = "SELECT "
			+ "Prefix.id, Prefix.name, Prefix.originId, Prefix.available, Prefix.created, Prefix.updated "
			+ "FROM Prefix "
			+ "WHERE Prefix.originId = ?";

	/*-------------------- FETCH ITEMS BY ID --------------------*/
	public static final String FETCH_VSUBNET_BY_ID = "SELECT "
			+ "id, name, label, description, info, type, created, updated FROM Vsubnet "
			+ "WHERE id = ?";
	public static final String FETCH_VNODE_BY_ID = "SELECT "
			+ "id, name, label, description, info, status, created, updated, "
			+ "posx, posy, location, type, vsubnetId, hwaddr, mgmtIp "
			+ "FROM `Vnode` WHERE Vnode.id = ?";
	public static final String FETCH_VLTP_BY_ID = "SELECT "
			+ "id, name, label, description, info, status, created, updated, vnodeId, port, bandwidth, mtu, busy "
			+ "FROM `Vltp` WHERE Vltp.id = ?";
	public static final String FETCH_VCTP_BY_ID = "SELECT "
			+ "id, name, label, description, info, created, updated, connType, connInfo, status, vltpId, vctpId, vnodeId "
			+ "FROM Vctp WHERE id = ?";
	public static final String FETCH_VLINK_BY_ID = "SELECT "
			+ "Vlink.id, Vlink.name, Vlink.label, Vlink.description, Vlink.info, Vlink.status, Vlink.created, Vlink.updated, "
			+ "Vlink.srcVltpId, Vlink.destVltpId, "
			+ "sLtp.vnodeId AS srcVnodeId, dLtp.vnodeId AS destVnodeId "
			+ "FROM ((Vlink INNER JOIN Vltp AS sLtp ON Vlink.srcVltpId=sLtp.id) INNER JOIN Vltp AS dLtp ON Vlink.destVltpId=dLtp.id) "
			+ "WHERE Vlink.id = ?";
	public static final String FETCH_VLINKCONN_BY_ID = "SELECT "
			+ "VlinkConn.id, VlinkConn.name, VlinkConn.label, VlinkConn.description, VlinkConn.info, VlinkConn.status, "
			+ "VlinkConn.created, VlinkConn.updated, VlinkConn.srcVctpId, VlinkConn.destVctpId, VlinkConn.vlinkId, "
			+ "sLtp.id AS srcVltpId, dLtp.id AS destVltpId, sLtp.vnodeId AS srcVnodeId, dLtp.vnodeId AS destVnodeId "
			+ "FROM ((VlinkConn "
			+ "INNER JOIN Vctp AS sCtp ON sCtp.id=srcVctpId INNER JOIN Vltp AS sLtp ON sLtp.id=sCtp.vltpId) "
			+ "INNER JOIN Vctp AS dCtp ON dCtp.id=destVctpId INNER JOIN Vltp AS dLtp ON dLtp.id=dCtp.vltpId) "
			+ "WHERE VlinkConn.id = ?";
	public static final String FETCH_VCONNECTION_BY_ID = "SELECT "
			+ "Vconnection.id, Vconnection.name, Vconnection.label, Vconnection.description, Vconnection.info, Vconnection.status, "
			+ "Vconnection.created, Vconnection.updated, Vconnection.srcVctpId, Vconnection.destVctpId, "
			+ "sCtp.vnodeId AS srcVnodeId, dCtp.vnodeId AS destVnodeId "
			+ "FROM ((Vconnection "
			+ "INNER JOIN Vctp AS sCtp ON Vconnection.srcVctpId=sCtp.id) "
			+ "INNER JOIN Vctp AS dCtp ON Vconnection.destVctpId=dCtp.id) "
			+ "WHERE Vconnection.id = ?";
	public static final String FETCH_VTRAIL_BY_ID = "SELECT "
			+ "Vtrail.id, Vtrail.name, Vtrail.label, Vtrail.description, Vtrail.info, Vtrail.status, Vtrail.created, Vtrail.updated, "
			+ "Vtrail.srcVnodeId, Vtrail.trgtVnodeId " 
			+ "FROM Vtrail "
			+ "WHERE id = ?";
	public static final String FETCH_VCROSS_CONNECT_BY_ID = "SELECT "
			+ "VcrossConnect.id, VcrossConnect.name, VcrossConnect.label, VcrossConnect.description, "
			+ "VcrossConnect.info, VcrossConnect.status, VcrossConnect.trailId, "
			+ "VcrossConnect.switchId, Vnode.mgmtIp as switchIpAddr, VcrossConnect.ingressPortId, VcrossConnect.egressPortId, "
			+ "VcrossConnect.created, VcrossConnect.updated "
			+ "FROM VcrossConnect INNER JOIN Vnode ON VcrossConnect.switchId = Vnode.id WHERE VcrossConnect.id = ?";
	public static final String FETCH_PREFIX_BY_ID = "SELECT "
			+ "id, name, originId, available, created, updated "
			+ "FROM Prefix WHERE id=?";

	/*-------------------- DELETE ITEMS BY ID --------------------*/
	public static final String DELETE_VSUBNET = "DELETE FROM Vsubnet WHERE id=?";
	public static final String DELETE_VNODE = "DELETE FROM Vnode WHERE id=?";
	public static final String DELETE_VLTP = "DELETE FROM Vltp WHERE id=?";
	public static final String DELETE_VCTP = "DELETE FROM Vctp WHERE id=?";
	public static final String DELETE_VLINK = "DELETE FROM Vlink WHERE id=?";
	public static final String DELETE_VLINKCONN = "DELETE FROM VlinkConn WHERE id=?";
	public static final String DELETE_VCONNECTION = "DELETE FROM Vconnection WHERE id=?";
	public static final String DELETE_VTRAIL = "DELETE FROM Vtrail WHERE id=?";
	public static final String DELETE_VCROSS_CONNECT = "DELETE FROM VcrossConnect WHERE id=?";
	public static final String DELETE_PREFIX = "DELETE FROM Prefix WHERE id=?";
	public static final String DELETE_PREFIX_BY_NAME = "DELETE FROM Prefix WHERE originId = ? AND name = ?";

	/*-------------------- UPDATE ITEMS BY ID --------------------*/
	public static final String UPDATE_VSUBNET = "UPDATE Vsubnet "
			+ "SET label=IFNULL(?, label), description=IFNULL(?, description), info=IFNULL(?, info) "
			+ "WHERE id = ?";
	public static final String UPDATE_VNODE = "UPDATE Vnode "
			+ "SET label=IFNULL(?, label), description=IFNULL(?, description), info=IFNULL(?, info), status=IFNULL(?, status), "
			+ "posx=IFNULL(?, posx), posy=IFNULL(?, posy), location=IFNULL(?, location), hwaddr=IFNULL(?, hwaddr), mgmtIp=IFNULL(?, mgmtIp) "
			+ "WHERE id = ?";
	public static final String UPDATE_VLTP = "UPDATE Vltp "
			+ "SET label=IFNULL(?, label), description=IFNULL(?, description), info=IFNULL(?, info), status=IFNULL(?, status), "
			+ "port=IFNULL(?, port), bandwidth=IFNULL(?, bandwidth), mtu=IFNULL(?, mtu), busy=IFNULL(?, busy) "
			+ "WHERE id = ?";
	public static final String UPDATE_VCTP = "UPDATE Vctp "
			+ "SET label=IFNULL(?, label), description=IFNULL(?, description), info=IFNULL(?, info), status=IFNULL(?, status) "
			+ "WHERE id = ?";
	public static final String UPDATE_VLINK = "UPDATE Vlink "
			+ "SET label=IFNULL(?, label), description=IFNULL(?, description), info=IFNULL(?, info), status=IFNULL(?, status), "
			+ "WHERE id = ?";
	public static final String UPDATE_VLINKCONN = "UPDATE VlinkConn "
			+ "SET label=IFNULL(?, label), description=IFNULL(?, description), info=IFNULL(?, info), status=IFNULL(?, status) "
			+ "WHERE id = ?";
	public static final String UPDATE_VCONNECTION = "UPDATE Vconnection "
			+ "SET label=IFNULL(?, label), description=IFNULL(?, description), info=IFNULL(?, info), status=IFNULL(?, status) "
			+ "WHERE id = ?";
	public static final String UPDATE_VTRAIL = "UPDATE Vtrail "
			+ "SET label=IFNULL(?, label), description=IFNULL(?, description), info=IFNULL(?, info), status=IFNULL(?, status) "
			+ "WHERE id = ?";
	public static final String UPDATE_VCROSS_CONNECT = "UPDATE VcrossConnect "
			+ "SET label=IFNULL(?, label), description=IFNULL(?, description), info=IFNULL(?, info), status=IFNULL(?, status) "
			+ "WHERE id = ?";

	/*-------------------- CROSSCONNECT GET AND CHECK INFO --------------------*/
	public static final String XC_CHECK_AND_GET_INFO = "SELECT "
			+ "Vnode.mgmtIp AS switchIpAddr, ingress.port AS ingressPort, egress.port AS egressPort "
			+ "FROM ((Vnode "
			+ "INNER JOIN Vltp AS ingress ON Vnode.id = ingress.vnodeId) "
			+ "INNER JOIN Vltp AS egress ON Vnode.id = egress.vnodeId) "
			+ "WHERE Vnode.id = ? AND Vnode.type = ? AND (ingress.id = ? AND egress.id = ?)";
	public static final String XC_GET_INFO = "SELECT "
			+ "Vnode.mgmtIp AS switchIpAddr, Vltp.port AS ingressPort "
			+ "FROM ((VcrossConnect "
			+ "INNER JOIN Vltp ON Vltp.id = VcrossConnect.ingressPortId) "
			+ "INNER JOIN Vnode ON Vnode.id = VcrossConnect.switchId) "
			+ "WHERE VcrossConnect.id = ?";
}
