package br.gov.lexml.madoc.server.catalog;

import br.gov.lexml.madoc.server.MadocException;

public class CatalogException extends MadocException {

	private static final long serialVersionUID = 1L;

	public CatalogException() {
	}

	public CatalogException(String message) {
		super(message);
	}

	public CatalogException(Throwable cause) {
		super(cause);
	}

	public CatalogException(String message, Throwable cause) {
		super(message, cause);
	}

}
