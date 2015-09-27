create or replace function numbers (minnumber in pls_integer, maxnumber in pls_integer)
  return sys.odcinumberlist pipelined is
  begin
    for i in minnumber .. maxnumber loop
      pipe row (i);
    end loop;
    return;
  end;
/