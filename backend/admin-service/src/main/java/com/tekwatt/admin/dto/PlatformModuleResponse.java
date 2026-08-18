package com.tekwatt.admin.dto;
import java.time.Instant;import java.util.List;
public record PlatformModuleResponse(String id,String name,String description,List<String>capabilities,List<String>dependencies,boolean available,boolean installed,boolean operational,String status,Instant installedAt){}
