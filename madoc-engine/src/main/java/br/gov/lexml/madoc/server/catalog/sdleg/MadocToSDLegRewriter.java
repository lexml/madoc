package br.gov.lexml.madoc.server.catalog.sdleg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.gov.lexml.madoc.server.catalog.store.Rewriter;

public class MadocToSDLegRewriter implements Rewriter {

	private final MadocUrnResolverHolder resolverHolder;

	private final Pattern madocUrnPattern = Pattern.compile("urn:sf:sistema;madoc:id;([^:]*)(?::version;([^:]*))?");

	public MadocToSDLegRewriter(MadocUrnResolverHolder resolverHolder) {
		super();
		this.resolverHolder = resolverHolder;
	}

	@Override
	public String rewriteUri(String docUri) {
		MadocUrnResolver resolver = resolverHolder.getResolver();
		Matcher m = madocUrnPattern.matcher(docUri);

		if(m != null && m.find()) {
			String modelId = m.group(1);
			return resolver.resolveMadocToUrn(modelId);
		}
		return null;
	}

}
