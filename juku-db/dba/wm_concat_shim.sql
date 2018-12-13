-- wm_concat is removed from oracle 12 version
-- this creates legacy.wm_concat aggregate function for backwards compatibility
-- see https://oracle-base.com/articles/misc/string-aggregation-techniques#wm_concat

create user legacy identified by legacy account lock;

CREATE OR REPLACE TYPE legacy.type_string_agg AS OBJECT
(
  g_string  VARCHAR2(32767),

  STATIC FUNCTION ODCIAggregateInitialize(sctx  IN OUT  type_string_agg)
    RETURN NUMBER,

  MEMBER FUNCTION ODCIAggregateIterate(self   IN OUT  type_string_agg,
                                       value  IN      VARCHAR2 )
     RETURN NUMBER,

  MEMBER FUNCTION ODCIAggregateTerminate(self         IN   type_string_agg,
                                         returnValue  OUT  VARCHAR2,
                                         flags        IN   NUMBER)
    RETURN NUMBER,

  MEMBER FUNCTION ODCIAggregateMerge(self  IN OUT  type_string_agg,
                                     ctx2  IN      type_string_agg)
    RETURN NUMBER
);
/


CREATE OR REPLACE TYPE BODY legacy.type_string_agg IS
  STATIC FUNCTION ODCIAggregateInitialize(sctx  IN OUT  type_string_agg)
    RETURN NUMBER IS
  BEGIN
    sctx := type_string_agg(NULL);
    RETURN ODCIConst.Success;
  END;

  MEMBER FUNCTION ODCIAggregateIterate(self   IN OUT  type_string_agg,
                                       value  IN      VARCHAR2 )
    RETURN NUMBER IS
  BEGIN
    SELF.g_string := self.g_string || ',' || value;
    RETURN ODCIConst.Success;
  END;

  MEMBER FUNCTION ODCIAggregateTerminate(self         IN   type_string_agg,
                                         returnValue  OUT  VARCHAR2,
                                         flags        IN   NUMBER)
    RETURN NUMBER IS
  BEGIN
    returnValue := RTRIM(LTRIM(SELF.g_string, ','), ',');
    RETURN ODCIConst.Success;
  END;

  MEMBER FUNCTION ODCIAggregateMerge(self  IN OUT  type_string_agg,
                                     ctx2  IN      type_string_agg)
    RETURN NUMBER IS
  BEGIN
    SELF.g_string := SELF.g_string || ',' || ctx2.g_string;
    RETURN ODCIConst.Success;
  END;
END;
/


CREATE OR REPLACE FUNCTION legacy.wm_concat (p_input VARCHAR2)
RETURN VARCHAR2
PARALLEL_ENABLE AGGREGATE USING type_string_agg;
/

create public synonym wm_concat for legacy.wm_concat;

grant execute on legacy.wm_concat to juku;