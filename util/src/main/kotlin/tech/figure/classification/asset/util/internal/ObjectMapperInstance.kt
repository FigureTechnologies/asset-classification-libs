package tech.figure.classification.asset.util.internal

import com.fasterxml.jackson.databind.ObjectMapper
import tech.figure.classification.asset.util.objects.ACObjectMapperUtil

internal val OBJECT_MAPPER: ObjectMapper by lazy { ACObjectMapperUtil.getObjectMapper() }
