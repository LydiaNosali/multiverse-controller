package io.nms.central.microservice.topology.impl;

public class InternalSql {
	
	public static final String LOCK_TABLES = "LOCK TABLES "
			+ "Vltp WRITE, Vltp AS sLtp WRITE, Vltp AS dLtp WRITE, "
			+ "Vctp WRITE, Vctp AS sCtp WRITE, Vctp AS dCtp WRITE, "
			+ "Vnode WRITE, Vlink WRITE, VlinkConn WRITE, Vconnection WRITE, "
			+ "Prefix WRITE";

	public static final String LOCK_TABLES_FOR_NODE = "LOCK TABLES "
			+ "Vltp WRITE, Vltp AS sLtp WRITE, Vltp AS dLtp WRITE, "
			+ "Vnode WRITE, Vlink WRITE";

	public static final String LOCK_TABLES_FOR_LINK = "LOCK TABLES "
			+ "Vltp WRITE, Vltp AS sLtp WRITE, Vltp AS dLtp WRITE, Vlink WRITE";
	
	public static final String LOCK_TABLES_FOR_LTP = "LOCK TABLES "
			+ "Vltp WRITE, Vltp AS sLtp WRITE, Vltp AS dLtp WRITE, Vlink WRITE";

	public static final String UPDATE_LTP_BUSY = "UPDATE Vltp SET busy=IFNULL(?, busy) WHERE id = ?";
	public static final String UPDATE_NODE_STATUS = "UPDATE Vnode SET status=IFNULL(?, status) WHERE id = ?";
	public static final String UPDATE_LTP_STATUS = "UPDATE Vltp SET status=IFNULL(?, status) WHERE id = ?";
	public static final String UPDATE_CTP_STATUS = "UPDATE Vctp SET status=IFNULL(?, status) WHERE id = ?";
	public static final String UPDATE_LINK_STATUS = "UPDATE Vlink SET status=IFNULL(?, status) WHERE id = ?";
	public static final String UPDATE_LC_STATUS = "UPDATE VlinkConn SET status=IFNULL(?, status) WHERE id = ?";
	public static final String UPDATE_CONNECTION_STATUS = "UPDATE Vconnection SET status=IFNULL(?, status) WHERE id = ?";
	public static final String UPDATE_PREFIX_STATUS_BY_NODE = "UPDATE Prefix SET available=IFNULL(?, available) WHERE originId = ?";
	
	// get node status
	public static final String GET_NODE_STATUS = "SELECT status FROM Vnode WHERE id = ?";

	// get all node IDs
	public static final String FETCH_ALL_NODE_IDS = "SELECT id FROM Vnode";
	
	// get the Link of an LTP
	public static final String FETCH_LINK_BY_LTP = "SELECT "
			+ "Vlink.id, Vlink.name "
			+ "FROM Vltp "
			+ "INNER JOIN Vlink ON Vltp.id=Vlink.srcVltpId OR Vltp.id=Vlink.destVltpId "
			+ "WHERE Vltp.id = ?";
	// get the LinkConn of a CTP
	public static final String FETCH_LC_BY_CTP = "SELECT "
			+ "VlinkConn.id, VlinkConn.name "
			+ "FROM Vctp "
			+ "INNER JOIN VlinkConn ON Vctp.id=VlinkConn.srcVctpId OR Vctp.id=VlinkConn.destVctpId "
			+ "WHERE Vctp.id = ? AND Vctp.connType = 'Ether'";
	// get the Connection of a CTP
	public static final String FETCH_CONNECTION_BY_CTP = "SELECT "
			+ "Vconnection.id, Vconnection.name "
			+ "FROM Vctp "
			+ "INNER JOIN Vconnection ON Vctp.id=Vconnection.srcVctpId OR Vctp.id=Vconnection.destVctpId "
			+ "WHERE Vctp.id = ? AND Vctp.connType != 'Ether'";

	// get status of the two LTPs of a Link
	public static final String FETCH_LINK_LTP_STATUS = "SELECT "
			+ "Vltp.id , Vltp.status "
			+ "FROM Vlink "
			+ "INNER JOIN Vltp ON Vltp.id=Vlink.srcVltpId OR Vltp.id=Vlink.destVltpId "
			+ "WHERE Vlink.id = ?";
	// get status of the two CTPs of a LinkConn
	public static final String FETCH_LC_CTP_STATUS = "SELECT "
			+ "Vctp.id , Vctp.status "
			+ "FROM VlinkConn "
			+ "INNER JOIN Vctp ON Vctp.id=VlinkConn.srcVctpId OR Vctp.id=VlinkConn.destVctpId "
			+ "WHERE VlinkConn.id = ?";
	// get status of the two CTPs of a Connection
	public static final String FETCH_CONNECTION_CTP_STATUS = "SELECT "
			+ "Vctp.id , Vctp.status "
			+ "FROM Vconnection "
			+ "INNER JOIN Vctp ON Vctp.id=Vconnection.srcVctpId OR Vctp.id=Vconnection.destVctpId "
			+ "WHERE Vconnection.id = ?";
	
	// get the nodeId of a CTP
	public static final String GET_CTP_NODE = "SELECT vnodeId from Vctp WHERE id = ?";
	// get the nodeId of a LTP
	public static final String GET_LTP_NODE = "SELECT vnodeId from Vltp WHERE id = ?";
	// get the type of a CTP
	public static final String GET_CTP_TYPE = "SELECT connType from Vctp WHERE id = ?";

	public static final String INIT_NODE_STATUS = "UPDATE Vnode SET status='DOWN'";
	public static final String INIT_LTP_STATUS = "UPDATE Vltp SET status='DOWN'";
	public static final String INIT_CTP_STATUS = "UPDATE Vctp SET status='DOWN'";
	public static final String INIT_LINK_STATUS = "UPDATE Vlink SET status='DOWN'";
	public static final String INIT_LC_STATUS = "UPDATE VlinkConn SET status='DOWN'";
	public static final String INIT_CONNECTION_STATUS = "UPDATE Vconnection SET status='DOWN'";
	public static final String INIT_TRAIL_STATUS = "UPDATE Vtrail SET status='DOWN'";
	public static final String INIT_CROSSCONNECT_STATUS = "UPDATE VcrossConnect SET status='DOWN'";
	public static final String INIT_PREFIX_STATUS = "UPDATE Prefix SET available=false";
	
	public static final String INSERT_IGNORE_VSUBNET = "INSERT IGNORE INTO Vsubnet (name, label, description, type, info) VALUES (?, ?, ?, ?, ?) ";

	/* public static final String UPDATE_PA_STATUS_BY_NODE = "UPDATE PrefixAnn, "
	+ "("  
	+ "    SELECT id " 
	+ "    FROM Vnode "
	+ "    WHERE Vnode.id = ? OR Vnode.name = ? "
	+ ") as n "
	+ "SET available=IFNULL(?, available) WHERE PrefixAnn.originId = n.id"; */
}