package fr.inria.jfbaget.irirefs;


import java.util.Objects;

import fr.inria.jfbaget.nanoparse.IMatch;
import fr.inria.jfbaget.nanoparse.matches.ListMatch;

public class IRIAuthority {
	
	String user = null;
	String host = null;
	Integer port = null;
	
	private static final String AUTHORITY_SEPARATOR = "//";
	private static final String USER_SEPARATOR = "@";
	private static final String PORT_SEPARATOR = ":";
			
	
	public IRIAuthority(ListMatch authorityMatch) {
		this.user = (String)authorityMatch.result().get(0).result();
		this.host = (String)authorityMatch.result().get(1).result();
		String hostkind = authorityMatch.result().get(1).reader().getName();
		if (hostkind.equals("IPv6address") || hostkind.equals("IPvFuture")) {
			this.host = "[" + this.host + "]";
		}
		this.port = (Integer)authorityMatch.result().get(2).result();
	}
	
	public IRIAuthority(IRIAuthority other) throws IllegalArgumentException {
		if (other == null) {
			throw new IllegalArgumentException("Cannot copy a null IAuthority.");
		}
		this.user = other.user;
		this.host = other.host;
		this.port = other.port;
	}
	
	public IRIAuthority(String user, String host, Integer port) {
		this.user = user;
		this.host = host;
		this.port = port;
	}
	
	public String getUser() {
		return this.user;
	}
	
	public String getHost() {
		return this.host;
	}
	
	public Integer getPort() {
		return this.port;
	}
	
	@Override
	public boolean equals(Object other) {
	    if (this == other) return true;
	    if (other == null || getClass() != other.getClass()) return false;
	    IRIAuthority that = (IRIAuthority) other;
	    return Objects.equals(this.user, that.user)
	        && Objects.equals(this.host, that.host)
	        && Objects.equals(this.port, that.port);
	}
	
	@Override
	public int hashCode() {
	    return Objects.hash(user, host, port);
	}
	
	public StringBuilder recompose(StringBuilder builder) {
		builder.append(AUTHORITY_SEPARATOR);
		this.recomposeUser(builder);
		this.recomposeHost(builder);
		this.recomposePort(builder);
		return builder;
	}
	
	public StringBuilder recomposeUser(StringBuilder builder) {
		if (this.user != null) {
			builder.append(this.user).append(USER_SEPARATOR);
		}
		return builder;
	}
	
	public StringBuilder recomposeHost(StringBuilder builder) {
		if (this.host != null) { // I don't know if this is necessary
			builder.append(this.host);
		}
		return builder;
	}
	
	public StringBuilder recomposePort(StringBuilder builder) {
		if (this.port != null) {
			builder.append(PORT_SEPARATOR).append(this.port);
		}
		return builder;
	}

}
