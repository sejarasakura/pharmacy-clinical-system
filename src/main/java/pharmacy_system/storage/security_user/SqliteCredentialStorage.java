package pharmacy_system.storage.security_user;

import pharmacy_system.model.security_user.Credential;
import pharmacy_system.model.security_user.PasswordHasher;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/** SQLite implementation of persistent authentication credentials. */
public class SqliteCredentialStorage implements CredentialStorage {
    private final SqliteDatabase database; private final PasswordHasher hasher;
    public SqliteCredentialStorage(SqliteDatabase database, PasswordHasher hasher){this.database=database;this.hasher=hasher;}
    public Credential create(Credential c){String q="INSERT INTO credentials(user_id,password_hash,failed_attempts,locked_until,password_updated_at,reset_token_hash,reset_token_expiry,version) VALUES(?,?,?,?,?,?,?,1)";try(Connection x=database.connection();PreparedStatement s=x.prepareStatement(q,Statement.RETURN_GENERATED_KEYS)){bind(s,c);s.executeUpdate();try(ResultSet k=s.getGeneratedKeys()){k.next();return findById(k.getLong(1)).orElseThrow();}}catch(SQLException e){throw fail(e);}}
    public Optional<Credential> findById(long id){return find("SELECT * FROM credentials WHERE credential_id=?",id);} public Optional<Credential> findByUserId(long id){return find("SELECT * FROM credentials WHERE user_id=?",id);}
    private Optional<Credential> find(String q,long id){try(Connection x=database.connection();PreparedStatement s=x.prepareStatement(q)){s.setLong(1,id);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(row(r)):Optional.empty();}}catch(SQLException e){throw fail(e);}}
    public boolean update(Credential c,long version){String q="UPDATE credentials SET user_id=?,password_hash=?,failed_attempts=?,locked_until=?,password_updated_at=?,reset_token_hash=?,reset_token_expiry=?,version=version+1 WHERE credential_id=? AND version=?";try(Connection x=database.connection();PreparedStatement s=x.prepareStatement(q)){bind(s,c);s.setLong(8,c.getCredentialId());s.setLong(9,version);return s.executeUpdate()==1;}catch(SQLException e){throw fail(e);}}
    public boolean delete(long id){try(Connection x=database.connection();PreparedStatement s=x.prepareStatement("DELETE FROM credentials WHERE credential_id=?")){s.setLong(1,id);return s.executeUpdate()==1;}catch(SQLException e){throw fail(e);}}
    private void bind(PreparedStatement s,Credential c)throws SQLException{s.setLong(1,c.getUserId());s.setString(2,c.getPasswordHash());s.setInt(3,c.getFailedAttempts());s.setString(4,time(c.getLockedUntil()));s.setString(5,time(c.getPasswordUpdatedAt()));s.setString(6,c.getResetTokenHash());s.setString(7,time(c.getResetTokenExpiry()));}
    private Credential row(ResultSet r)throws SQLException{return new Credential(r.getLong("credential_id"),r.getLong("user_id"),r.getString("password_hash"),r.getInt("failed_attempts"),parse(r.getString("locked_until")),parse(r.getString("password_updated_at")),parse(r.getString("reset_token_expiry")),r.getString("reset_token_hash"),hasher,r.getLong("version"));}
    private static String time(LocalDateTime t){return t==null?null:t.toString();} private static LocalDateTime parse(String t){return t==null?null:LocalDateTime.parse(t);} private static IllegalStateException fail(SQLException e){return new IllegalStateException("Could not persist credential",e);}
}
