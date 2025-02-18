package com.tntexfinance.crm.integration.reborn;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CurlMessage {
  private String curlCommand;
  private String jsonPayload;
  private String url;
  private String method;
}
