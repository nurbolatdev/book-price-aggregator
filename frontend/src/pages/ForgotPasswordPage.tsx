import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { forgotPassword } from "../api/auth";
import { getErrorMessage } from "../api/errors";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { Field, controlAttrs } from "../components/ui/Field";
import { Input } from "../components/ui/Input";
import { AuthCard } from "../features/auth/AuthCard";
import { RateLimitAlert } from "../features/auth/RateLimitAlert";
import { authLinkClasses } from "../features/auth/authLink";
import { emailSchema } from "../features/auth/schemas";
import { useRateLimit } from "../features/auth/useRateLimit";
import { useSingleSubmit } from "../features/auth/useSingleSubmit";
import { applyFieldErrors } from "../lib/applyFieldErrors";
import { formatCountdown } from "../lib/format";
import { usePageMeta } from "../lib/usePageMeta";

interface ForgotValues {
  email: string;
}

const BACK_LINK = (
  <p className="text-center text-sm">
    <Link to="/login" className={`${authLinkClasses} font-semibold`}>
      Вернуться ко входу
    </Link>
  </p>
);

export default function ForgotPasswordPage() {
  usePageMeta({
    title: "Восстановление пароля | KitapRadar",
    description: "Отправим ссылку для нового пароля на почту аккаунта.",
  });
  const rateLimit = useRateLimit();
  const [sentTo, setSentTo] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<ForgotValues>({
    resolver: zodResolver(emailSchema),
    defaultValues: { email: "" },
  });
  const mutation = useMutation({
    mutationFn: forgotPassword,
    meta: { silentError: true },
  });

  const submit = useSingleSubmit(
    handleSubmit(async (values) => {
      setFormError(null);
      try {
        await mutation.mutateAsync(values.email);
        setSentTo(values.email);
      } catch (error) {
        if (rateLimit.handle(error)) return;
        if (applyFieldErrors(error, setError, ["email"])) return;
        setFormError(getErrorMessage(error));
      }
    }),
  );

  if (sentTo) {
    return (
      <AuthCard
        icon="mail"
        titleSize="status"
        align="start"
        title="Проверьте почту"
        focusTitle
      >
        <p className="leading-[1.6] text-ink-soft">
          Если аккаунт с адресом <strong>{sentTo}</strong> существует, мы
          отправили на него ссылку. Она действует 1 час.
        </p>
        <p className="text-sm text-muted">
          Ответ одинаковый для любого адреса — так мы не раскрываем, кто
          зарегистрирован.
        </p>
        <p className="text-sm">
          <Link to="/login" className={`${authLinkClasses} font-semibold`}>
            Вернуться ко входу
          </Link>
        </p>
      </AuthCard>
    );
  }

  return (
    <AuthCard
      title="Восстановление пароля"
      description="Укажите email аккаунта — пришлём ссылку для нового пароля."
    >
      <form
        noValidate
        onSubmit={(event) => void submit(event)}
        className="flex flex-col gap-4"
      >
        {formError && <Alert tone="danger">{formError}</Alert>}
        <RateLimitAlert rateLimit={rateLimit} />
        <Field label="Email" error={errors.email?.message}>
          {(props) => (
            <Input
              {...controlAttrs(props)}
              inputSize="lg"
              type="email"
              autoComplete="email"
              autoCapitalize="none"
              spellCheck={false}
              {...register("email")}
            />
          )}
        </Field>
        <Button
          type="submit"
          size="form"
          className="w-full"
          loading={mutation.isPending}
          disabled={rateLimit.blocked}
        >
          {rateLimit.blocked
            ? `Отправить ссылку · ${formatCountdown(rateLimit.secondsLeft)}`
            : "Отправить ссылку"}
        </Button>
      </form>
      {BACK_LINK}
    </AuthCard>
  );
}
