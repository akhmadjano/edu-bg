package com.platform.service;

import com.platform.entity.Payment;
import com.platform.entity.User;
import com.platform.repository.PaymentRepository;
import com.platform.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TelegramBotService extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.admin.ids:}")
    private String adminIdsStr;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${payment.card.number:8600 0000 0000 0000}")
    private String cardNumber;

    @Value("${payment.card.owner:Kurs egasi}")
    private String cardOwner;

    @Value("${payment.amount:50000}")
    private Long paymentAmount;

    @Value("${payment.premium.days:30}")
    private int premiumDays;

    private final UserRepository    userRepo;
    private final PaymentRepository paymentRepo;
    private final Map<Long, String> userState = new ConcurrentHashMap<>();

    public TelegramBotService(@Value("${telegram.bot.token}") String botToken,
                              UserRepository userRepo,
                              PaymentRepository paymentRepo) {
        super(botToken);
        this.userRepo    = userRepo;
        this.paymentRepo = paymentRepo;
    }

    // ── BOT NI QO'LDA REGISTER QILISH ─────────────────────────
    // telegrambots-spring-boot-starter ba'zida avtomatik ishlamaydi
    // @PostConstruct orqali ishonchli register qilamiz
    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            log.info("✅ Telegram bot muvaffaqiyatli ulandi: @{}", botUsername);
        } catch (TelegramApiException e) {
            log.error("❌ Telegram bot ulanmadi: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (!update.hasMessage()) return;
            Message msg  = update.getMessage();
            Long chatId  = msg.getChatId();
            String text  = msg.hasText() ? msg.getText().trim() : "";

            if (msg.hasPhoto()) { handlePhoto(msg); return; }

            if      (text.startsWith("/start"))              handleStart(msg);
            else if (text.equals("💳 Premium sotib olish")
                    || text.equals("/premium"))                handlePremium(chatId);
            else if (text.equals("🌐 Saytga kirish"))        sendPlain(chatId, "Sayt: " + frontendUrl);
            else if (text.equals("📞 Yordam"))               handleHelp(chatId);
            else if (text.startsWith("/link "))              handleLink(msg);
            else if (text.startsWith("/approve "))           handleApprove(msg, true);
            else if (text.startsWith("/reject "))            handleApprove(msg, false);
            else if (text.equals("/pending"))                handlePending(chatId);
            else if (text.equals("/users"))                  handleAdminUsers(chatId);
            else                                             sendKeyboard(chatId, "Tugmalardan birini tanlang:");
        } catch (Exception e) {
            log.error("Bot xatosi: {}", e.getMessage(), e);
        }
    }

    private void handleStart(Message msg) {
        Long chatId  = msg.getChatId();
        String text  = msg.getText().trim();
        String fname = msg.getFrom().getFirstName();

        if (text.contains(" ")) {
            String param = text.split(" ", 2)[1].trim();
            String phoneNumber = decodeStartParam(param);

            Optional<User> uOpt = userRepo.findByPhoneNumber(phoneNumber);
            if (uOpt.isEmpty()) {
                sendPlain(chatId, "Telefon raqam topilmadi: " + phoneNumber +
                        "\nAvval saytda ro'yxatdan o'ting: " + frontendUrl +
                        "\n\nYoki: /link +998901234567");
                return;
            }
            User u = uOpt.get();
            u.setTelegramId(chatId);
            userRepo.save(u);
            log.info("Telegram bog'landi: userId={}, chatId={}", u.getId(), chatId);
            sendKeyboard(chatId, "Akkounting muvaffaqiyatli bog'landi!\n👤 " + u.getFullName() +
                    "\n\nEndi /premium orqali obuna sotib olishingiz mumkin!");
            return;
        }

        sendKeyboard(chatId, "Salom, " + fname + "!\n\n" +
                "EduPlatform botiga xush kelibsiz!\n\n" +
                "Akkountingizni bog'lash uchun:\n" +
                "Saytda Profil sahifasiga o'ting → Telegram tugmasini bosing\n\n" +
                "Yoki qo'lda: /link +998901234567");
    }

    /**
     * Saytdan ?start=BASE64 ko'rinishida keladi (URL-safe base64, padding yo'q)
     * Qo'lda /link +998901234567 yuborilsa — + bor, decode shart emas
     */
    private String decodeStartParam(String param) {
        if (param.startsWith("+") || param.matches("^[0-9]{9,15}$")) return param;
        try {
            String base64 = param.replace('-', '+').replace('_', '/');
            int pad = base64.length() % 4;
            if (pad == 2) base64 += "==";
            else if (pad == 3) base64 += "=";
            byte[] decoded = java.util.Base64.getDecoder().decode(base64);
            return new String(decoded);
        } catch (Exception e) {
            log.warn("Start param decode xatosi: '{}'", param);
            return param;
        }
    }

    private void handlePremium(Long chatId) {
        Optional<User> uOpt = userRepo.findByTelegramId(chatId);
        if (uOpt.isEmpty()) {
            sendPlain(chatId, "Avval akkountingizni bog'lang:\n/link +998901234567\n\nYoki saytda: Profil → Telegram bog'lash");
            return;
        }
        User u = uOpt.get();
        if (u.hasActivePremium()) {
            String until = u.getPremiumUntil() != null ? u.getPremiumUntil().toLocalDate().toString() : "Abadiy";
            sendPlain(chatId, "Sizda premium allaqachon bor!\nMuddati: " + until);
            return;
        }
        userState.put(chatId, "WAITING_RECEIPT");
        sendPlain(chatId,
                "PREMIUM OBUNA\n\n" +
                        "Narx: " + paymentAmount + " so'm\n" +
                        "Muddat: " + premiumDays + " kun\n\n" +
                        "TO'LOV QADAMLARI:\n" +
                        "1. Quyidagi kartaga pul o'tkazing\n" +
                        "2. Chek (screenshot) ni shu botga yuboring\n" +
                        "3. Admin 1-24 soat ichida tasdiqlaydi\n\n" +
                        "Karta: " + cardNumber + "\n" +
                        "Egasi: " + cardOwner + "\n\n" +
                        "Chekni yuboring!");
    }

    private void handlePhoto(Message msg) {
        Long chatId = msg.getChatId();
        if (!"WAITING_RECEIPT".equals(userState.getOrDefault(chatId, ""))) {
            sendPlain(chatId, "Rasm olindi. Premium uchun: /premium");
            return;
        }
        Optional<User> uOpt = userRepo.findByTelegramId(chatId);
        if (uOpt.isEmpty()) {
            sendPlain(chatId, "Akkounting bog'lanmagan. /start bosing.");
            return;
        }

        String fileId = msg.getPhoto().stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .map(PhotoSize::getFileId).orElse(null);

        User user = uOpt.get();
        Payment p = Payment.builder()
                .user(user).telegramId(chatId).amount(paymentAmount)
                .receiptFileId(fileId)
                .receiptNote(msg.getCaption() != null ? msg.getCaption() : null)
                .premiumDays(premiumDays).status(Payment.Status.PENDING).build();
        p = paymentRepo.save(p);
        userState.remove(chatId);

        sendPlain(chatId, "Chek qabul qilindi!\nTo'lov raqami: #" + p.getId() + "\nAdmin tez orada ko'rib chiqadi.");
        notifyAdminsNewPayment(p);
    }

    private void handleLink(Message msg) {
        Long chatId = msg.getChatId();
        String[] parts = msg.getText().split(" ", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            sendPlain(chatId, "Format: /link +998901234567");
            return;
        }
        String phoneNumber = parts[1].trim();
        Optional<User> uOpt = userRepo.findByPhoneNumber(phoneNumber);
        if (uOpt.isEmpty()) { sendPlain(chatId, "Telefon raqam topilmadi: " + phoneNumber); return; }
        User u = uOpt.get();
        u.setTelegramId(chatId);
        userRepo.save(u);
        sendKeyboard(chatId, "Akkounting bog'landi!\n" + u.getFullName() + " - " + phoneNumber);
    }

    private void handleHelp(Long chatId) {
        sendPlain(chatId, "YORDAM\n\n" +
                "/premium — Premium sotib olish\n" +
                "/link +998XXXXXXXXX — Akkount bog'lash\n" +
                "/start — Boshlash");
    }

    private void handlePending(Long chatId) {
        if (!isAdmin(chatId)) { sendPlain(chatId, "Ruxsat yo'q"); return; }
        var list = paymentRepo.findByStatusOrderByCreatedAtDesc(Payment.Status.PENDING);
        if (list.isEmpty()) { sendPlain(chatId, "Kutilayotgan to'lovlar yo'q"); return; }
        for (Payment p : list) {
            String info = "To'lov #" + p.getId() + "\n" +
                    p.getUser().getFullName() + " (" + p.getUser().getPhoneNumber() + ")\n" +
                    p.getAmount() + " so'm\n" +
                    (p.getCreatedAt() != null ? p.getCreatedAt().toLocalDate() + "\n" : "") +
                    (p.getReceiptNote() != null ? p.getReceiptNote() + "\n" : "") +
                    "\n/approve " + p.getId() + "\n/reject " + p.getId();
            if (p.getReceiptFileId() != null) sendPhoto(chatId, p.getReceiptFileId(), info);
            else sendPlain(chatId, info);
        }
    }

    private void handleAdminUsers(Long chatId) {
        if (!isAdmin(chatId)) { sendPlain(chatId, "Ruxsat yo'q"); return; }
        long total   = userRepo.count();
        long premium = userRepo.findAll().stream().filter(User::hasActivePremium).count();
        sendPlain(chatId, "Foydalanuvchilar:\nJami: " + total + "\nPremium: " + premium + "\nOddiy: " + (total - premium));
    }

    private void handleApprove(Message msg, boolean approve) {
        Long chatId = msg.getChatId();
        if (!isAdmin(chatId)) { sendPlain(chatId, "Ruxsat yo'q"); return; }
        try {
            Long payId = Long.parseLong(msg.getText().split(" ")[1].trim());
            Payment p  = paymentRepo.findById(payId).orElse(null);
            if (p == null) { sendPlain(chatId, "To'lov #" + payId + " topilmadi"); return; }
            if (p.getStatus() != Payment.Status.PENDING) {
                sendPlain(chatId, "Allaqachon ko'rib chiqilgan: " + p.getStatus()); return;
            }
            p.setStatus(approve ? Payment.Status.APPROVED : Payment.Status.REJECTED);
            p.setApprovedByAdminId(chatId);
            p.setReviewedAt(LocalDateTime.now());
            if (approve) {
                User u = p.getUser();
                u.setPremium(true);
                u.setPremiumUntil(LocalDateTime.now().plusDays(p.getPremiumDays()));
                userRepo.save(u);
                sendPlain(chatId, "Tasdiqlandi! Premium " + p.getPremiumDays() + " kunga berildi. #" + payId);
                if (u.getTelegramId() != null)
                    sendPlain(u.getTelegramId(), "To'lovingiz tasdiqlandi! " +
                            p.getPremiumDays() + " kunlik premium faol!\nSayt: " + frontendUrl);
            } else {
                sendPlain(chatId, "Rad etildi. #" + payId);
                if (p.getTelegramId() != null)
                    sendPlain(p.getTelegramId(), "To'lovingiz tasdiqlanmadi. Qayta urinib ko'ring.");
            }
            paymentRepo.save(p);
        } catch (NumberFormatException e) {
            sendPlain(chatId, "Format: /approve 5");
        }
    }

    // ── PUBLIC metodlar (PaymentService chaqiradi) ─────────────

    public void notifyAdminsNewPayment(Payment payment) {
        String info = "Yangi to'lov #" + payment.getId() + "\n" +
                payment.getUser().getFullName() + " (" + payment.getUser().getPhoneNumber() + ")\n" +
                payment.getAmount() + " so'm\n" +
                "/approve " + payment.getId() + "\n/reject " + payment.getId();
        for (Long adminId : getAdminIds()) {
            if (payment.getReceiptFileId() != null) sendPhoto(adminId, payment.getReceiptFileId(), info);
            else sendPlain(adminId, info);
        }
    }

    public void sendPlain(Long chatId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.error("sendPlain xato (chatId={}): {}", chatId, e.getMessage());
        }
    }

    // ── PRIVATE yordamchi metodlar ──────────────────────────────

    private void sendKeyboard(Long chatId, String text) {
        try {
            ReplyKeyboardMarkup kb = ReplyKeyboardMarkup.builder()
                    .resizeKeyboard(true)
                    .keyboard(List.of(
                            new KeyboardRow(List.of(
                                    KeyboardButton.builder().text("💳 Premium sotib olish").build()
                            )),
                            new KeyboardRow(List.of(
                                    KeyboardButton.builder().text("🌐 Saytga kirish").build(),
                                    KeyboardButton.builder().text("📞 Yordam").build()
                            ))
                    )).build();
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .replyMarkup(kb)
                    .build());
        } catch (TelegramApiException e) {
            sendPlain(chatId, text);
        }
    }

    private void sendPhoto(Long chatId, String fileId, String caption) {
        try {
            execute(SendPhoto.builder()
                    .chatId(chatId.toString())
                    .photo(new InputFile(fileId))
                    .caption(caption)
                    .build());
        } catch (TelegramApiException e) {
            sendPlain(chatId, caption);
        }
    }

    private boolean isAdmin(Long chatId) { return getAdminIds().contains(chatId); }

    private List<Long> getAdminIds() {
        if (adminIdsStr == null || adminIdsStr.isBlank()) return List.of();
        return Arrays.stream(adminIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    try { return Long.parseLong(s); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}