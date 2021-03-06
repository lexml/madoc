package br.gov.lexml.madoc.server.schema.parser;

import br.gov.lexml.madoc.server.MadocException;

public class SaveException extends MadocException {

	private static final long serialVersionUID = 1L;

	public SaveException() {
	}

	public SaveException(String message) {
		super(message);
	}

	public SaveException(Throwable cause) {
		super(cause);
	}

	public SaveException(String message, Throwable cause) {
		super(message, cause);
	}

}
