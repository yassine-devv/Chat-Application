CREATE TABLE public.users (
    id SERIAL PRIMARY KEY,
    username TEXT NOT NULL,
    password TEXT NOT NULL
);

CREATE TABLE public.chats (
    id SERIAL PRIMARY KEY,
    private BOOLEAN NOT NULL
);

CREATE TABLE public.participants (
    id SERIAL PRIMARY KEY,
    id_user INT NOT NULL,
    id_chat INT NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY(id_user) REFERENCES public.users(id),
    CONSTRAINT fk_chat FOREIGN KEY(id_chat) REFERENCES public.chats(id)
);

CREATE TABLE public.messages (
    id SERIAL PRIMARY KEY,
    consumer INT NOT NULL,
    producer INT NOT NULL,
    content TEXT NOT NULL,
    type_message TEXT NOT NULL,
    chat INT NOT NULL,
    CONSTRAINT fk_consumer_mess FOREIGN KEY(consumer) REFERENCES public.users(id),
    CONSTRAINT fk_producer_mess FOREIGN KEY(producer) REFERENCES public.users(id),
    CONSTRAINT fk_chat_mess FOREIGN KEY(chat) REFERENCES public.chats(id)
);

CREATE TYPE status_invitation AS ENUM('PENDING', 'ACCEPTED', 'REFIUSED');

CREATE TABLE public.invitations (
    id SERIAL PRIMARY KEY,
    inviter INT NOT NULL,
    invitee INT NOT NULL,
    status status_invitation NOT NULL,
    CONSTRAINT fk_inviter FOREIGN KEY(inviter) REFERENCES public.users(id),
    CONSTRAINT fk_invitee FOREIGN KEY(invitee) REFERENCES public.users(id)
)
