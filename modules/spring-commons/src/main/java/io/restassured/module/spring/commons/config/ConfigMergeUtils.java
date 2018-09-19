package io.restassured.module.spring.commons.config;

/**
 * @author Olga Maciaszek-Sharma
 */
public class ConfigMergeUtils {

	public static SpecificationConfig mergeConfig(SpecificationConfig base, SpecificationConfig toMerge) {
		boolean thisIsUserConfigured = base.isUserConfigured();
		boolean otherIsUserConfigured = toMerge.isUserConfigured();
		if (!otherIsUserConfigured) {
			return base;
		}
		if (thisIsUserConfigured) {
			if (toMerge.getDecoderConfig().isUserConfigured()) {
				base = base.decoderConfig(toMerge.getDecoderConfig());
			}

			if (toMerge.getEncoderConfig().isUserConfigured()) {
				base = base.encoderConfig(toMerge.getEncoderConfig());
			}

			if (toMerge.getHeaderConfig().isUserConfigured()) {
				base = base.headerConfig(toMerge.getHeaderConfig());
			}

			if (toMerge.getJsonConfig().isUserConfigured()) {
				base = base.jsonConfig(toMerge.getJsonConfig());
			}

			if (toMerge.getLogConfig().isUserConfigured()) {
				base = base.logConfig(toMerge.getLogConfig());
			}

			if (toMerge.getObjectMapperConfig().isUserConfigured()) {
				base = base.objectMapperConfig(toMerge.getObjectMapperConfig());
			}

			if (toMerge.getSessionConfig().isUserConfigured()) {
				base = base.sessionConfig(toMerge.getSessionConfig());
			}

			if (toMerge.getXmlConfig().isUserConfigured()) {
				base = base.xmlConfig(toMerge.getXmlConfig());
			}

			if (toMerge.getAsyncConfig().isUserConfigured()) {
				base = base.asyncConfig(toMerge.getAsyncConfig());
			}

			if (toMerge.getMultiPartConfig().isUserConfigured()) {
				base = base.multiPartConfig(toMerge.getMultiPartConfig());
			}

			if (toMerge.getClientConfig().isUserConfigured()) {
				base = base.clientConfig(toMerge.getClientConfig());
			}

			if (toMerge.getParamConfig().isUserConfigured()) {
				base = base.paramConfig(toMerge.getParamConfig());
			}

			return base;
		} else {
			return toMerge;
		}
	}
}
