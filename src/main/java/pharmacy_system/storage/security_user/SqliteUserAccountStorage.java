package pharmacy_system.storage.security_user;

import pharmacy_system.model.security_user.AccountStatus;
import pharmacy_system.model.security_user.UserAccount;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** SQLite implementation of durable user-account storage. */
public class SqliteUserAccountStorage implements UserAccountStorage {
    private final SqliteDatabase database;
    public SqliteUserAccountStorage(SqliteDatabase database) { this.database = database; }
    public UserAccount create(UserAccount a) { String q="INSERT INTO user_accounts(username,email,status,approved,created_at,updated_at,last_login_at,disabled_at,disabled_reason,version) VALUES(?,?,?,?,?,?,?,?,?,1)"; try(Connection c=database.connection(); PreparedStatement s=c.prepareStatement(q,Statement.RETURN_GENERATED_KEYS)){ bind(s,a); s.executeUpdate(); try(ResultSet k=s.getGeneratedKeys()){k.next(); return findById(k.getLong(1)).orElseThrow();} }catch(SQLException e){throw new IllegalStateException("Could not save user account",e);} }
    public Optional<UserAccount> findById(long id){return find("SELECT * FROM user_accounts WHERE user_id=?",id);}
    public Optional<UserAccount> findByUsername(String value){return find("SELECT * FROM user_accounts WHERE username=?",value);}
    public Optional<UserAccount> findByEmail(String value){return find("SELECT * FROM user_accounts WHERE email=?",value);}
    private Optional<UserAccount> find(String q,Object value){try(Connection c=database.connection(); PreparedStatement s=c.prepareStatement(q)){s.setObject(1,value);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(row(r)):Optional.empty();}}catch(SQLException e){throw new IllegalStateException("Could not read user account",e);}}
    public boolean update(UserAccount a,long version){String q="UPDATE user_accounts SET username=?,email=?,status=?,approved=?,created_at=?,updated_at=?,last_login_at=?,disabled_at=?,disabled_reason=?,version=version+1 WHERE user_id=? AND version=?";try(Connection c=database.connection();PreparedStatement s=c.prepareStatement(q)){bind(s,a);s.setLong(10,a.getUserId());s.setLong(11,version);return s.executeUpdate()==1;}catch(SQLException e){throw new IllegalStateException("Could not update user account",e);}}
    public boolean delete(long id){try(Connection c=database.connection();PreparedStatement s=c.prepareStatement("DELETE FROM user_accounts WHERE user_id=?")){s.setLong(1,id);return s.executeUpdate()==1;}catch(SQLException e){throw new IllegalStateException("Could not delete user account",e);}}
    public List<UserAccount> listAll(){List<UserAccount> result=new ArrayList<>();try(Connection c=database.connection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM user_accounts ORDER BY user_id")){while(r.next())result.add(row(r));return result;}catch(SQLException e){throw new IllegalStateException("Could not list user accounts",e);}}
    public List<Map<String,Object>> queryForReport(Map<String,String> ignored){List<Map<String,Object>> out=new ArrayList<>();for(UserAccount a:listAll()){Map<String,Object> row=new HashMap<>();row.put("userId",a.getUserId());row.put("username",a.getUsername());row.put("email",a.getEmail());row.put("status",a.getStatus());row.put("createdAt",a.getCreatedAt());row.put("lastLoginAt",a.getLastLoginAt());out.add(row);}return out;}
    private void bind(PreparedStatement s,UserAccount a)throws SQLException{s.setString(1,a.getUsername());s.setString(2,a.getEmail());s.setString(3,a.getStatus().name());s.setInt(4,a.isRegistrationApproved()?1:0);s.setString(5,t(a.getCreatedAt()));s.setString(6,t(a.getUpdatedAt()));s.setString(7,t(a.getLastLoginAt()));s.setString(8,t(a.getDisabledAt()));s.setString(9,a.getDisabledReason());}
    private UserAccount row(ResultSet r)throws SQLException{return new UserAccount(r.getLong("user_id"),r.getString("username"),r.getString("email"),AccountStatus.valueOf(r.getString("status")),r.getInt("approved")!=0,p(r.getString("created_at")),p(r.getString("updated_at")),p(r.getString("last_login_at")),p(r.getString("disabled_at")),r.getString("disabled_reason"),r.getLong("version"));}
    private static String t(LocalDateTime value){return value==null?null:value.toString();} private static LocalDateTime p(String value){return value==null?null:LocalDateTime.parse(value);}
}
